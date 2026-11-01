using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace YekPanel.Services;

public sealed record ConnectionSummary(
    string Adapter,
    string Ipv4,
    string Gateway,
    string Dns,
    string Speed,
    string Wifi);

public sealed record DriveUsage(string Name, double UsedGb, double TotalGb, double Percent);

public sealed record BatteryInfo(string Text, string Detail);

public sealed record PingSample(
    string Host,
    string Address,
    int Sent,
    int Received,
    long Min,
    long Avg,
    long Max);

public static class ToolsService
{
    public static ConnectionSummary GetConnection()
    {
        string adapter = "—";
        string ipv4 = "—";
        string gateway = "—";
        string dns = "—";
        string speed = "";

        try
        {
            foreach (var nic in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (nic.OperationalStatus != OperationalStatus.Up ||
                    nic.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
                    continue;

                if (IsVirtualAdapter(nic))
                    continue;

                var props = nic.GetIPProperties();
                var address = props.UnicastAddresses
                    .FirstOrDefault(a => a.Address.AddressFamily == AddressFamily.InterNetwork &&
                                         !IPAddress.IsLoopback(a.Address));
                if (address == null)
                    continue;

                var kind = nic.NetworkInterfaceType == NetworkInterfaceType.Wireless80211
                    ? "Wi-Fi"
                    : "Ethernet";
                adapter = string.Equals(nic.Name, kind, StringComparison.OrdinalIgnoreCase)
                    ? kind
                    : $"{kind} · {nic.Name}";
                ipv4 = address.Address.ToString();

                var gw = props.GatewayAddresses
                    .FirstOrDefault(g => g.Address.AddressFamily == AddressFamily.InterNetwork);
                if (gw != null)
                    gateway = gw.Address.ToString();

                var servers = props.DnsAddresses
                    .Where(d => d.AddressFamily == AddressFamily.InterNetwork)
                    .Select(d => d.ToString())
                    .ToArray();
                if (servers.Length > 0)
                    dns = string.Join(", ", servers);

                try
                {
                    speed = nic.Speed > 0 ? $"{nic.Speed / 1_000_000d:0} Mbps" : "";
                }
                catch
                {
                    speed = "";
                }

                break;
            }
        }
        catch
        {
        }

        return new ConnectionSummary(adapter, ipv4, gateway, dns, speed, GetWifi());
    }

    public static bool IsVirtualAdapter(NetworkInterface nic)
    {
        var text = $"{nic.Name} {nic.Description}";
        string[] markers =
        {
            "radmin", "vpn", "tap-", "tap ", "virtual", "vmware", "virtualbox",
            "hyper-v", "vethernet", "hamachi", "tailscale", "zerotier", "wireguard",
            "nordlynx", "bluetooth", "teredo", "isatap", "loopback"
        };

        return markers.Any(marker => text.Contains(marker, StringComparison.OrdinalIgnoreCase));
    }

    public static string GetWifi()
    {
        try
        {
            var info = new ProcessStartInfo("netsh", "wlan show interfaces")
            {
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true,
                UseShellExecute = false
            };

            using var process = Process.Start(info);
            if (process == null)
                return "kablosuz bilgi yok";

            var output = process.StandardOutput.ReadToEnd();
            process.WaitForExit(1500);

            string? ssid = null;
            string? signal = null;
            string? state = null;

            foreach (var raw in output.Split('\n'))
            {
                var line = raw.Trim();
                var index = line.IndexOf(':');
                if (index <= 0)
                    continue;

                var key = line[..index].Trim();
                var value = line[(index + 1)..].Trim();
                if (value.Length == 0)
                    continue;

                if (key.Equals("SSID", StringComparison.OrdinalIgnoreCase))
                    ssid = value;
                else if (key.Contains("Signal", StringComparison.OrdinalIgnoreCase) ||
                         key.Contains("Sinyal", StringComparison.OrdinalIgnoreCase))
                    signal = value;
                else if (key.Equals("State", StringComparison.OrdinalIgnoreCase))
                    state = value;
            }

            if (!string.IsNullOrWhiteSpace(ssid))
                return string.IsNullOrWhiteSpace(signal) ? ssid : $"{ssid} · {signal}";

            return state?.Contains("disconnected", StringComparison.OrdinalIgnoreCase) == true ||
                   state?.Contains("bağlı değil", StringComparison.OrdinalIgnoreCase) == true
                ? "Wi-Fi bağlı değil"
                : "kablosuz bilgi yok";
        }
        catch
        {
            return "kablosuz bilgi yok";
        }
    }

    public static async Task<string?> GetPublicIpAsync(HttpClient http, CancellationToken ct)
    {
        foreach (var url in new[] { "https://api.ipify.org", "https://ifconfig.me/ip" })
        {
            try
            {
                var text = (await http.GetStringAsync(url, ct)).Trim();
                if (IPAddress.TryParse(text, out var address))
                    return address.ToString();
            }
            catch
            {
            }
        }

        return null;
    }

    public static IReadOnlyList<DriveUsage> GetDrives()
    {
        var result = new List<DriveUsage>();
        try
        {
            foreach (var drive in DriveInfo.GetDrives())
            {
                try
                {
                    if (!drive.IsReady ||
                        drive.DriveType is not (DriveType.Fixed or DriveType.Removable))
                        continue;

                    var total = drive.TotalSize / 1024d / 1024d / 1024d;
                    if (total < 0.1)
                        continue;

                    var free = drive.AvailableFreeSpace / 1024d / 1024d / 1024d;
                    var used = Math.Max(0, total - free);
                    result.Add(new DriveUsage(drive.Name.TrimEnd('\\'), used, total, used / total * 100));
                }
                catch
                {
                }
            }
        }
        catch
        {
        }

        return result;
    }

    public static BatteryInfo GetBattery()
    {
        try
        {
            var power = System.Windows.Forms.SystemInformation.PowerStatus;
            if (power.BatteryChargeStatus.HasFlag(System.Windows.Forms.BatteryChargeStatus.NoSystemBattery))
                return new BatteryInfo("pil yok", "Masaüstü / güç kaynağı");

            var percent = power.BatteryLifePercent;
            if (percent > 1)
                return new BatteryInfo("—", "Pil bilgisi okunamadı");

            var value = (int)Math.Round(percent * 100);
            var charging = power.PowerLineStatus == System.Windows.Forms.PowerLineStatus.Online;
            var status = charging ? "prizde" : "pilde";

            string detail = charging ? "Şarj oluyor / güç bağlı" : "Pil ile çalışıyor";
            if (power.BatteryLifeRemaining > 0)
            {
                var remaining = TimeSpan.FromSeconds(power.BatteryLifeRemaining);
                detail += $" · {remaining.Hours} sa {remaining.Minutes:00} dk kaldı";
            }

            return new BatteryInfo($"%{value} · {status}", detail);
        }
        catch
        {
            return new BatteryInfo("—", "Pil bilgisi okunamadı");
        }
    }

    public static async Task<PingSample?> PingAsync(string host, int count = 4)
    {
        host = (host ?? "").Trim();
        if (host.Length == 0)
            return null;

        string address = "—";
        try
        {
            var addresses = await Dns.GetHostAddressesAsync(host);
            var v4 = addresses.FirstOrDefault(a => a.AddressFamily == AddressFamily.InterNetwork);
            if (v4 != null)
                address = v4.ToString();
        }
        catch
        {
        }

        var times = new List<long>();
        int sent = 0;

        try
        {
            using var ping = new Ping();
            for (int i = 0; i < Math.Clamp(count, 1, 8); i++)
            {
                sent++;
                try
                {
                    var reply = await ping.SendPingAsync(host, 2000);
                    if (reply.Status == IPStatus.Success)
                    {
                        times.Add(reply.RoundtripTime);
                        address = reply.Address.ToString();
                    }
                }
                catch
                {
                }
            }
        }
        catch
        {
        }

        if (sent == 0)
            return null;

        var min = times.Count > 0 ? times.Min() : 0;
        var max = times.Count > 0 ? times.Max() : 0;
        var avg = times.Count > 0 ? (long)times.Average() : 0;
        return new PingSample(host, address, sent, times.Count, min, avg, max);
    }

    public static async Task<string> ResolveAsync(string host, CancellationToken ct)
    {
        host = (host ?? "").Trim();
        if (host.Length == 0)
            return "adres girin";

        try
        {
            var addresses = await Dns.GetHostAddressesAsync(host, ct);
            if (addresses.Length == 0)
                return "kayıt bulunamadı";

            return string.Join(", ", addresses
                .Where(a => a.AddressFamily == AddressFamily.InterNetwork)
                .Select(a => a.ToString()));
        }
        catch
        {
            return "çözümlenemedi";
        }
    }

    public static async Task<bool> IsPortOpenAsync(string host, int port, CancellationToken ct)
    {
        host = (host ?? "").Trim();
        if (host.Length == 0 || port is < 1 or > 65535)
            return false;

        try
        {
            using var client = new TcpClient();
            var connect = client.ConnectAsync(host, port, ct).AsTask();
            var completed = await Task.WhenAny(connect, Task.Delay(3500, ct));
            if (completed != connect)
                return false;

            await connect;
            return client.Connected;
        }
        catch
        {
            return false;
        }
    }

    public static void Launch(string file)
    {
        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = file,
                UseShellExecute = true
            });
        }
        catch
        {
        }
    }

    public static string? FindDataFile(string name)
    {
        try
        {
            var start = new DirectoryInfo(AppContext.BaseDirectory);
            for (int i = 0; i < 6 && start != null; i++)
            {
                var candidate = Path.Combine(start.FullName, name);
                if (File.Exists(candidate))
                    return candidate;
                start = start.Parent;
            }

            var current = Path.Combine(Directory.GetCurrentDirectory(), name);
            return File.Exists(current) ? current : null;
        }
        catch
        {
            return null;
        }
    }
}
