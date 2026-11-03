using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;
using Microsoft.Win32;

namespace YekPanel.Services;

/// <summary>
/// YEK Panel Android uygulamasının aynı yerel ağdan bağlanmasını sağlayan
/// gömülü köprü. Veriler yalnızca LAN üzerinde, 6 haneli PIN ile sunulur.
/// </summary>
public sealed class BridgeServer : IDisposable
{
    public const int DefaultPort = 8787;
    public const int DiscoveryPort = 8788;
    public const string AppName = "YEK Panel PC Köprüsü";
    private const int Protocol = 1;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    private sealed class AuthEntry
    {
        public int Count { get; set; }
        public DateTime At { get; set; }
        public DateTime? Until { get; set; }
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct SystemPowerStatus
    {
        public byte AcLineStatus;
        public byte BatteryFlag;
        public byte BatteryLifePercent;
        public byte SystemStatusFlag;
        public int BatteryLifeTime;
        public int BatteryFullLifeTime;
    }

    [DllImport("kernel32.dll")]
    private static extern bool GetSystemPowerStatus(out SystemPowerStatus status);

    private readonly Func<double?>? _temperatureProvider;
    private readonly SystemMonitor _system = new();
    private readonly NetworkMeter _network = new();
    private readonly object _sampleGate = new();
    private readonly object _authGate = new();
    private readonly Dictionary<string, AuthEntry> _authFails = new(StringComparer.OrdinalIgnoreCase);

    private TcpListener? _listener;
    private UdpClient? _discovery;
    private CancellationTokenSource? _cts;
    private Task? _httpTask;
    private Task? _discoveryTask;
    private string _pin = "";
    private int _port = DefaultPort;
    private string _osName = "";
    private volatile bool _running;

    public BridgeServer(Func<double?>? temperatureProvider = null)
    {
        _temperatureProvider = temperatureProvider;
        // İlk istek boş kalmasın: örnekleyiciyi başlangıçta ısıt.
        _system.Read();
        _network.Sample();
    }

    public bool IsRunning => _running;

    public int Port => _port;

    public string Pin => _pin;

    public string? LastError { get; private set; }

    public string? Warning { get; private set; }

    public event Action? StateChanged;

    public static string NewPin()
    {
        Span<byte> bytes = stackalloc byte[4];
        RandomNumberGenerator.Fill(bytes);
        var value = BitConverter.ToUInt32(bytes);
        return (100000 + (value % 900000)).ToString();
    }

    public static IReadOnlyList<string> LocalAddresses()
    {
        var list = new List<string>();
        try
        {
            foreach (var nic in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (nic.OperationalStatus != OperationalStatus.Up)
                    continue;
                if (nic.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
                    continue;
                if (ToolsService.IsVirtualAdapter(nic))
                    continue;

                foreach (var address in nic.GetIPProperties().UnicastAddresses)
                {
                    if (address.Address.AddressFamily != AddressFamily.InterNetwork)
                        continue;
                    var text = address.Address.ToString();
                    if (text.StartsWith("127.") || text.StartsWith("169.254."))
                        continue;
                    if (!list.Contains(text))
                        list.Add(text);
                }
            }
        }
        catch
        {
        }

        return list;
    }

    public bool Start(int port = DefaultPort, string? pin = null)
    {
        Stop();

        if (port is < 1024 or > 65535)
        {
            LastError = "Port 1024–65535 arasında olmalı";
            StateChanged?.Invoke();
            return false;
        }

        _port = port;
        _pin = string.IsNullOrWhiteSpace(pin) ? NewPin() : pin.Trim();
        LastError = null;
        Warning = null;

        try
        {
            _listener = new TcpListener(IPAddress.Any, port);
            _listener.Start(20);
        }
        catch (Exception ex)
        {
            LastError = ex is SocketException
                ? $"Port {port} açılamadı. Başka bir köprü veya uygulama kullanıyor olabilir."
                : "Köprü başlatılamadı: " + ex.Message;
            _listener = null;
            StateChanged?.Invoke();
            return false;
        }

        try
        {
            _discovery = new UdpClient(DiscoveryPort) { EnableBroadcast = true };
        }
        catch
        {
            _discovery = null;
            Warning = $"UDP {DiscoveryPort} kullanılamıyor; otomatik bulma çalışmayabilir.";
        }

        _cts = new CancellationTokenSource();
        _httpTask = Task.Run(() => RunHttpAsync(_cts.Token));
        if (_discovery != null)
            _discoveryTask = Task.Run(() => RunDiscoveryAsync(_cts.Token));

        _running = true;
        StateChanged?.Invoke();
        return true;
    }

    public void Stop()
    {
        var wasRunning = _running;
        _running = false;

        try
        {
            _cts?.Cancel();
        }
        catch
        {
        }

        try
        {
            _listener?.Stop();
        }
        catch
        {
        }

        try
        {
            _discovery?.Close();
        }
        catch
        {
        }

        try
        {
            _httpTask?.Wait(1500);
            _discoveryTask?.Wait(1500);
        }
        catch
        {
        }

        _cts?.Dispose();
        _cts = null;
        _listener = null;
        _discovery = null;
        _httpTask = null;
        _discoveryTask = null;

        if (wasRunning)
            StateChanged?.Invoke();
    }

    public void Dispose() => Stop();

    private async Task RunHttpAsync(CancellationToken token)
    {
        while (!token.IsCancellationRequested)
        {
            TcpClient client;
            try
            {
                client = await _listener!.AcceptTcpClientAsync(token);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (ObjectDisposedException)
            {
                break;
            }
            catch (SocketException)
            {
                if (token.IsCancellationRequested)
                    break;
                await Task.Delay(100, token);
                continue;
            }
            catch
            {
                break;
            }

            _ = Task.Run(() => HandleClientAsync(client, token), token);
        }
    }

    private async Task HandleClientAsync(TcpClient client, CancellationToken token)
    {
        using (client)
        {
            try
            {
                client.ReceiveTimeout = 5000;
                client.SendTimeout = 5000;
                var remote = (client.Client.RemoteEndPoint as IPEndPoint)?.Address.ToString() ?? "";
                if (remote.StartsWith("::ffff:", StringComparison.Ordinal))
                    remote = remote[7..];

                var stream = client.GetStream();
                using var reader = new StreamReader(
                    stream, Encoding.ASCII, detectEncodingFromByteOrderMarks: false,
                    bufferSize: 4096, leaveOpen: true);

                var requestLine = await reader.ReadLineAsync(token);
                if (string.IsNullOrWhiteSpace(requestLine))
                    return;

                var headers = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
                for (var i = 0; i < 100; i++)
                {
                    var line = await reader.ReadLineAsync(token);
                    if (line is null || line.Length == 0)
                        break;
                    var separator = line.IndexOf(':');
                    if (separator > 0)
                        headers[line[..separator].Trim()] = line[(separator + 1)..].Trim();
                }

                var parts = requestLine.Split(' ', StringSplitOptions.RemoveEmptyEntries);
                if (parts.Length < 2)
                {
                    await WriteResponseAsync(stream, 400,
                        "application/json; charset=utf-8",
                        "{\"ok\":false,\"error\":\"bad-request\"}", token);
                    return;
                }

                var method = parts[0].ToUpperInvariant();
                var target = parts[1];
                var path = target;
                var query = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
                var question = target.IndexOf('?');
                if (question >= 0)
                {
                    path = target[..question];
                    foreach (var pair in target[(question + 1)..].Split('&'))
                    {
                        var equals = pair.IndexOf('=');
                        if (equals <= 0)
                            continue;
                        query[Uri.UnescapeDataString(pair[..equals])] =
                            Uri.UnescapeDataString(pair[(equals + 1)..]);
                    }
                }

                if (path == "/api/ping")
                {
                    await WriteResponseAsync(stream, 200,
                        "application/json; charset=utf-8", BuildPingJson(), token);
                    return;
                }

                if (path == "/api/status")
                {
                    if (!string.Equals(method, "GET", StringComparison.Ordinal))
                    {
                        await WriteResponseAsync(stream, 405,
                            "application/json; charset=utf-8",
                            "{\"ok\":false,\"error\":\"method\"}", token);
                        return;
                    }

                    var tokenValue = headers.TryGetValue("X-Yek-Token", out var headerToken)
                        ? headerToken
                        : query.TryGetValue("token", out var queryToken) ? queryToken : "";

                    var auth = CheckAuth(remote, tokenValue);
                    if (auth == "locked")
                    {
                        await WriteResponseAsync(stream, 429,
                            "application/json; charset=utf-8",
                            "{\"ok\":false,\"error\":\"locked\"}", token);
                        return;
                    }

                    if (auth != "ok")
                    {
                        await WriteResponseAsync(stream, 401,
                            "application/json; charset=utf-8",
                            "{\"ok\":false,\"error\":\"pin\"}", token);
                        return;
                    }

                    await WriteResponseAsync(stream, 200,
                        "application/json; charset=utf-8", BuildStatusJson(), token);
                    return;
                }

                if (path is "/" or "/index.html")
                {
                    await WriteResponseAsync(stream, 200,
                        "text/html; charset=utf-8", BuildInfoPage(), token);
                    return;
                }

                await WriteResponseAsync(stream, 404,
                    "application/json; charset=utf-8",
                    "{\"ok\":false,\"error\":\"not-found\"}", token);
            }
            catch
            {
                // Bağlantı koptu veya istek bozuk; sunucu çalışmaya devam eder.
            }
        }
    }

    private string CheckAuth(string remote, string token)
    {
        lock (_authGate)
        {
            var now = DateTime.UtcNow;
            _authFails.TryGetValue(remote, out var entry);

            if (entry?.Until is DateTime until && now < until)
                return "locked";

            if (token.Length > 0 && string.Equals(token, _pin, StringComparison.Ordinal))
            {
                _authFails.Remove(remote);
                return "ok";
            }

            var count = entry != null && (now - entry.At).TotalSeconds < 60
                ? entry.Count + 1
                : 1;
            _authFails[remote] = new AuthEntry
            {
                Count = count,
                At = now,
                Until = count >= 5 ? now.AddMinutes(1) : null
            };

            return count >= 5 ? "locked" : "bad";
        }
    }

    private string BuildPingJson() => JsonSerializer.Serialize(new
    {
        ok = true,
        app = AppName,
        version = AppInfo.VersionFull,
        name = Environment.MachineName,
        port = _port,
        protocol = Protocol
    }, JsonOptions);

    private string BuildStatusJson()
    {
        double cpuPercent;
        ulong ramTotal;
        ulong ramUsed;
        int ramPercent;
        long netDown;
        long netUp;
        long linkMbps;
        List<string> ips;
        int batteryPercent;
        bool batteryCharging;
        double? tempC;

        lock (_sampleGate)
        {
            var snapshot = _system.Read();
            var (down, up) = _network.Sample();
            cpuPercent = Math.Round(snapshot.CpuPercent, 1);
            ramTotal = snapshot.TotalRamBytes;
            ramUsed = snapshot.TotalRamBytes > snapshot.AvailableRamBytes
                ? snapshot.TotalRamBytes - snapshot.AvailableRamBytes
                : 0;
            ramPercent = ramTotal > 0
                ? (int)Math.Round(ramUsed * 100.0 / ramTotal)
                : 0;
            netDown = (long)Math.Max(0, down);
            netUp = (long)Math.Max(0, up);
            linkMbps = GetLinkMbps();
            ips = LocalAddresses().ToList();
        }

        ReadBattery(out batteryPercent, out batteryCharging);
        tempC = ReadTemperature();

        var payload = new
        {
            ok = true,
            app = AppName,
            version = AppInfo.VersionFull,
            name = Environment.MachineName,
            os = GetOsName(),
            uptimeSec = Environment.TickCount64 / 1000,
            cpuPercent,
            ramTotal,
            ramUsed,
            ramPercent,
            disks = GetDisks(),
            netDown,
            netUp,
            linkMbps,
            ips,
            batteryPercent,
            batteryCharging,
            tempC,
            processes = GetProcesses(),
            ts = DateTimeOffset.UtcNow.ToUnixTimeSeconds()
        };

        return JsonSerializer.Serialize(payload, JsonOptions);
    }

    private double? ReadTemperature()
    {
        try
        {
            return _temperatureProvider?.Invoke();
        }
        catch
        {
            return null;
        }
    }

    private static void ReadBattery(out int percent, out bool charging)
    {
        percent = -1;
        charging = false;
        try
        {
            if (!GetSystemPowerStatus(out var status))
                return;
            if (status.BatteryLifePercent != 255)
                percent = status.BatteryLifePercent;
            charging = status.AcLineStatus == 1 &&
                (status.BatteryFlag & 8) != 0;
        }
        catch
        {
        }
    }

    private static long GetLinkMbps()
    {
        long best = 0;
        try
        {
            foreach (var nic in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (nic.OperationalStatus != OperationalStatus.Up)
                    continue;
                if (nic.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
                    continue;
                if (ToolsService.IsVirtualAdapter(nic))
                    continue;
                try
                {
                    var speed = nic.Speed;
                    if (speed > best && speed < 100_000_000_000)
                        best = speed;
                }
                catch
                {
                }
            }
        }
        catch
        {
        }

        return (long)Math.Round(best / 1_000_000.0);
    }

    private static List<object> GetDisks()
    {
        var list = new List<object>();
        try
        {
            foreach (var drive in DriveInfo.GetDrives())
            {
                try
                {
                    if (!drive.IsReady || drive.DriveType != DriveType.Fixed)
                        continue;
                    var total = drive.TotalSize;
                    var free = drive.AvailableFreeSpace;
                    list.Add(new
                    {
                        name = drive.Name.TrimEnd('\\'),
                        total,
                        free,
                        percent = total > 0 ? (int)Math.Round((total - free) * 100.0 / total) : 0
                    });
                }
                catch
                {
                }
            }
        }
        catch
        {
        }

        return list;
    }

    private static List<object> GetProcesses()
    {
        var list = new List<object>();
        try
        {
            foreach (var process in Process.GetProcesses()
                         .OrderByDescending(p => SafeWorkingSet(p))
                         .Take(5))
            {
                try
                {
                    list.Add(new
                    {
                        pid = process.Id,
                        name = process.ProcessName,
                        memMb = Math.Round(process.WorkingSet64 / 1024.0 / 1024.0, 1)
                    });
                }
                catch
                {
                }
                finally
                {
                    process.Dispose();
                }
            }
        }
        catch
        {
        }

        return list;
    }

    private static long SafeWorkingSet(Process process)
    {
        try
        {
            return process.WorkingSet64;
        }
        catch
        {
            return 0;
        }
    }

    private string GetOsName()
    {
        if (_osName.Length > 0)
            return _osName;

        try
        {
            using var key = Registry.LocalMachine.OpenSubKey(
                @"SOFTWARE\Microsoft\Windows NT\CurrentVersion");
            var product = key?.GetValue("ProductName") as string ?? "";
            var display = key?.GetValue("DisplayVersion") as string ?? "";
            var buildText = key?.GetValue("CurrentBuild") as string ?? "";
            product = product.Replace("Microsoft ", "", StringComparison.Ordinal);
            if (int.TryParse(buildText, out var build) && build >= 22000)
                product = product.Replace("Windows 10", "Windows 11", StringComparison.Ordinal);
            _osName = string.Join(" ",
                new[] { product, display, buildText }.Where(part => !string.IsNullOrWhiteSpace(part)));
        }
        catch
        {
            _osName = RuntimeInformation.OSDescription;
        }

        if (_osName.Length == 0)
            _osName = RuntimeInformation.OSDescription;

        return _osName;
    }

    private string BuildInfoPage() => """
<!doctype html>
<html lang="tr"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>YEK Panel PC Köprüsü</title>
<style>
body{font-family:system-ui,sans-serif;background:#08111e;color:#e8f1ff;
max-width:620px;margin:40px auto;padding:0 20px;line-height:1.6}
h1{font-size:22px;color:#22d3ee}code{background:#111e31;padding:3px 7px;
border-radius:6px;color:#22d3ee}.muted{color:#8ea4c3}
</style></head><body>
<h1>YEK Panel PC Köprüsü</h1>
<p class="muted">Bu adres yalnızca yerel ağda çalışır. Android uygulamasında
PC sekmesine bu bilgisayarın IP adresini ve PIN'i girin.</p>
<p>Durum: <code>GET /api/status</code> · PIN gerekli<br>
Keşif: <code>UDP 8788</code></p>
</body></html>
""";

    private async Task RunDiscoveryAsync(CancellationToken token)
    {
        var udp = _discovery;
        if (udp == null)
            return;

        while (!token.IsCancellationRequested)
        {
            try
            {
                var result = await udp.ReceiveAsync(token);
                var text = Encoding.UTF8.GetString(result.Buffer).Trim();
                if (!string.Equals(text, "YEK-PANEL-DISCOVER", StringComparison.Ordinal))
                    continue;

                var bytes = Encoding.UTF8.GetBytes(BuildPingJson());
                await udp.SendAsync(bytes, result.RemoteEndPoint, token);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (ObjectDisposedException)
            {
                break;
            }
            catch (SocketException)
            {
                if (token.IsCancellationRequested)
                    break;
                try
                {
                    await Task.Delay(200, token);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
            }
            catch
            {
                try
                {
                    await Task.Delay(200, token);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
            }
        }
    }

    private static async Task WriteResponseAsync(
        NetworkStream stream, int status, string contentType, string body,
        CancellationToken token)
    {
        var bytes = Encoding.UTF8.GetBytes(body);
        var reason = status switch
        {
            200 => "OK",
            400 => "Bad Request",
            401 => "Unauthorized",
            404 => "Not Found",
            405 => "Method Not Allowed",
            429 => "Too Many Requests",
            500 => "Internal Server Error",
            _ => "OK"
        };

        var head = $"HTTP/1.1 {status} {reason}\r\n" +
                   $"Content-Type: {contentType}\r\n" +
                   $"Content-Length: {bytes.Length}\r\n" +
                   "Connection: close\r\n" +
                   "Cache-Control: no-store\r\n" +
                   "X-Content-Type-Options: nosniff\r\n" +
                   "Access-Control-Allow-Origin: *\r\n\r\n";
        var headBytes = Encoding.ASCII.GetBytes(head);

        await stream.WriteAsync(headBytes, token);
        await stream.WriteAsync(bytes, token);
        await stream.FlushAsync(token);
    }
}
