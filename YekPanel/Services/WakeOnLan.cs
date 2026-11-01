using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace YekPanel.Services;

public static class WakeOnLan
{
    public static bool TryParseMac(string? text, out byte[] mac)
    {
        mac = new byte[6];
        var hex = new string((text ?? "").Where(Uri.IsHexDigit).ToArray());
        if (hex.Length != 12)
            return false;

        for (int i = 0; i < 6; i++)
            mac[i] = Convert.ToByte(hex.Substring(i * 2, 2), 16);

        return mac.Any(b => b != 0x00) && mac.Any(b => b != 0xFF);
    }

    public static string Normalize(string? text)
    {
        if (!TryParseMac(text, out var mac))
            return "";

        return string.Join(":", mac.Select(b => b.ToString("X2")));
    }

    public static bool Send(string? macText)
    {
        if (!TryParseMac(macText, out var mac))
            return false;

        var packet = new byte[102];
        for (int i = 0; i < 6; i++)
            packet[i] = 0xFF;
        for (int repeat = 0; repeat < 16; repeat++)
            Buffer.BlockCopy(mac, 0, packet, 6 + repeat * 6, 6);

        try
        {
            using var udp = new UdpClient { EnableBroadcast = true };
            udp.Send(packet, packet.Length, new IPEndPoint(IPAddress.Broadcast, 9));
            udp.Send(packet, packet.Length, new IPEndPoint(IPAddress.Broadcast, 7));

            foreach (var broadcast in GetDirectedBroadcasts())
            {
                try
                {
                    udp.Send(packet, packet.Length, new IPEndPoint(broadcast, 9));
                }
                catch
                {
                }
            }

            return true;
        }
        catch
        {
            return false;
        }
    }

    private static IEnumerable<IPAddress> GetDirectedBroadcasts()
    {
        var list = new List<IPAddress>();

        try
        {
            foreach (var nic in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (nic.OperationalStatus != OperationalStatus.Up ||
                    nic.NetworkInterfaceType == NetworkInterfaceType.Loopback)
                    continue;

                foreach (var unicast in nic.GetIPProperties().UnicastAddresses)
                {
                    if (unicast.Address.AddressFamily != AddressFamily.InterNetwork ||
                        unicast.IPv4Mask is null)
                        continue;

                    var address = unicast.Address.GetAddressBytes();
                    var mask = unicast.IPv4Mask.GetAddressBytes();
                    if (address.Length != 4 || mask.Length != 4)
                        continue;

                    var broadcast = new byte[4];
                    for (int i = 0; i < 4; i++)
                        broadcast[i] = (byte)(address[i] | ~mask[i]);

                    list.Add(new IPAddress(broadcast));
                }
            }
        }
        catch
        {
        }

        return list;
    }
}
