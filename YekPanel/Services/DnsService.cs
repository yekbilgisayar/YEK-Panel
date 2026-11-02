using System.Diagnostics;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Security.Principal;

namespace YekPanel.Services;

public sealed record DnsProvider(string Key, string Name, string[] Servers);

/// <summary>Aktif bağlantının DNS sunucularını okur ve netsh ile değiştirir.</summary>
public static class DnsService
{
    public static readonly DnsProvider[] Providers =
    {
        new("cloudflare", "Cloudflare", new[] { "1.1.1.1", "1.0.0.1" }),
        new("google", "Google", new[] { "8.8.8.8", "8.8.4.4" }),
        new("opendns", "OpenDNS", new[] { "208.67.222.222", "208.67.220.220" })
    };

    public static bool IsAdministrator
    {
        get
        {
            try
            {
                using var identity = WindowsIdentity.GetCurrent();
                var principal = new WindowsPrincipal(identity);
                return principal.IsInRole(WindowsBuiltInRole.Administrator);
            }
            catch
            {
                return false;
            }
        }
    }

    public static (string Adapter, string Dns) GetCurrent()
    {
        try
        {
            foreach (var nic in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (nic.OperationalStatus != OperationalStatus.Up ||
                    nic.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
                    continue;
                if (ToolsService.IsVirtualAdapter(nic))
                    continue;

                var props = nic.GetIPProperties();
                var hasIpv4 = props.UnicastAddresses.Any(a =>
                    a.Address.AddressFamily == AddressFamily.InterNetwork &&
                    !IPAddress.IsLoopback(a.Address));
                if (!hasIpv4)
                    continue;

                var servers = props.DnsAddresses
                    .Where(address => address.AddressFamily == AddressFamily.InterNetwork)
                    .Select(address => address.ToString())
                    .Distinct()
                    .ToArray();

                return (nic.Name, servers.Length > 0 ? string.Join(", ", servers) : "otomatik (DHCP)");
            }
        }
        catch
        {
        }

        return ("—", "—");
    }

    public static (bool Ok, string Message) Apply(string adapterName, IReadOnlyList<string> servers)
    {
        if (string.IsNullOrWhiteSpace(adapterName) || adapterName == "—")
            return (false, "Aktif bağlantı bulunamadı");

        if (!IsAdministrator)
            return (false, "DNS değiştirmek için yönetici izni gerekir (tepsi > Yönetici olarak yeniden başlat)");

        try
        {
            if (servers.Count == 0)
            {
                var (ok, output) = Run("interface", "ipv4", "set", "dnsservers",
                    $"name={adapterName}", "source=dhcp");
                return (ok, ok ? "DNS otomatik (DHCP) olarak ayarlandı" : FirstLine(output));
            }

            var (primaryOk, primaryOutput) = Run("interface", "ipv4", "set", "dnsservers",
                $"name={adapterName}", "source=static", $"address={servers[0]}", "register=primary");
            if (!primaryOk)
                return (false, FirstLine(primaryOutput));

            for (var i = 1; i < servers.Count; i++)
            {
                Run("interface", "ipv4", "add", "dnsservers",
                    $"name={adapterName}", $"address={servers[i]}", $"index={i + 1}");
            }

            return (true, $"DNS ayarlandı: {string.Join(", ", servers)}");
        }
        catch (Exception ex)
        {
            AppLog.Error("DnsService", ex);
            return (false, ex.Message);
        }
    }

    private static (bool Ok, string Output) Run(params string[] arguments)
    {
        var info = new ProcessStartInfo("netsh")
        {
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true
        };

        foreach (var argument in arguments)
            info.ArgumentList.Add(argument);

        using var process = Process.Start(info);
        if (process == null)
            return (false, "netsh başlatılamadı");

        var output = process.StandardOutput.ReadToEnd() + process.StandardError.ReadToEnd();
        process.WaitForExit(6000);
        return (process.ExitCode == 0, output.Trim());
    }

    private static string FirstLine(string text)
    {
        var line = (text ?? "").Split('\n').Select(part => part.Trim())
            .FirstOrDefault(part => part.Length > 0);
        return line ?? "Bilinmeyen hata";
    }
}
