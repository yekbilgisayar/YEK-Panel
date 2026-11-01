using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace YekPanel.Services;

public sealed class NetworkMeter
{
    private long _lastIn;
    private long _lastOut;
    private long _lastTicks = Environment.TickCount64;
    private bool _started;

    public (double Down, double Up) Sample()
    {
        long received = 0;
        long sent = 0;

        try
        {
            foreach (var adapter in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (adapter.OperationalStatus != OperationalStatus.Up)
                    continue;
                if (adapter.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
                    continue;
                if (ToolsService.IsVirtualAdapter(adapter))
                    continue;

                try
                {
                    var stats = adapter.GetIPStatistics();
                    received += stats.BytesReceived;
                    sent += stats.BytesSent;
                }
                catch
                {
                }
            }
        }
        catch
        {
        }

        var now = Environment.TickCount64;
        var seconds = Math.Max(0.2, (now - _lastTicks) / 1000d);

        if (!_started)
        {
            _started = true;
            _lastIn = received;
            _lastOut = sent;
            _lastTicks = now;
            return (0, 0);
        }

        var down = Math.Max(0, (received - _lastIn) / seconds);
        var up = Math.Max(0, (sent - _lastOut) / seconds);

        _lastIn = received;
        _lastOut = sent;
        _lastTicks = now;
        return (down, up);
    }
}

public static class NetworkMonitor
{
    public static string GetLocalIp()
    {
        try
        {
            foreach (var adapter in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (adapter.OperationalStatus != OperationalStatus.Up)
                    continue;
                if (adapter.NetworkInterfaceType == NetworkInterfaceType.Loopback)
                    continue;
                if (ToolsService.IsVirtualAdapter(adapter))
                    continue;

                foreach (var address in adapter.GetIPProperties().UnicastAddresses)
                {
                    if (address.Address.AddressFamily == AddressFamily.InterNetwork &&
                        !IPAddress.IsLoopback(address.Address))
                        return address.Address.ToString();
                }
            }
        }
        catch
        {
        }

        return "—";
    }

    public static async Task<long?> PingAsync(string host)
    {
        try
        {
            using var ping = new Ping();
            var reply = await ping.SendPingAsync(host, 2500);
            return reply.Status == IPStatus.Success ? reply.RoundtripTime : null;
        }
        catch
        {
            return null;
        }
    }
}
