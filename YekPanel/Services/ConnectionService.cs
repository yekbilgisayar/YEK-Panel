using System.Diagnostics;
using System.Net;
using System.Runtime.InteropServices;

namespace YekPanel.Services;

public sealed record ConnectionEntry(
    int Pid,
    string Process,
    string Local,
    string Remote,
    string State,
    bool Listening);

/// <summary>Windows TCP tablosunu (netstat hafif) süreç adlarıyla okur.</summary>
public static class ConnectionService
{
    private const int AfInet = 2;
    private const int AfInet6 = 23;
    private const int TcpTableOwnerPidAll = 5;

    [StructLayout(LayoutKind.Sequential)]
    private struct TcpRowOwnerPid
    {
        public uint State;
        public uint LocalAddr;
        public uint LocalPort;
        public uint RemoteAddr;
        public uint RemotePort;
        public uint OwningPid;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct Tcp6RowOwnerPid
    {
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 16)]
        public byte[] LocalAddr;
        public uint LocalScopeId;
        public uint LocalPort;
        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 16)]
        public byte[] RemoteAddr;
        public uint RemoteScopeId;
        public uint RemotePort;
        public uint State;
        public uint OwningPid;
    }

    [DllImport("iphlpapi.dll", SetLastError = true)]
    private static extern uint GetExtendedTcpTable(
        IntPtr tcpTable,
        ref int size,
        bool order,
        int addressFamily,
        int tableClass,
        uint reserved);

    public static IReadOnlyList<ConnectionEntry> GetConnections(bool establishedOnly)
    {
        var result = new List<ConnectionEntry>();
        var names = new Dictionary<int, string>();

        try
        {
            result.AddRange(ReadTable(AfInet, ipv6: false, names));
            result.AddRange(ReadTable(AfInet6, ipv6: true, names));
        }
        catch (Exception ex)
        {
            AppLog.Error("ConnectionService", ex);
        }

        IEnumerable<ConnectionEntry> query = result;
        if (establishedOnly)
            query = query.Where(entry => entry.State == "ESTABLISHED");

        return query
            .OrderByDescending(entry => entry.State == "ESTABLISHED")
            .ThenBy(entry => entry.Process, StringComparer.OrdinalIgnoreCase)
            .ThenBy(entry => entry.Remote, StringComparer.Ordinal)
            .Take(300)
            .ToList();
    }

    private static IEnumerable<ConnectionEntry> ReadTable(
        int family,
        bool ipv6,
        Dictionary<int, string> names)
    {
        var size = 0;
        var first = GetExtendedTcpTable(IntPtr.Zero, ref size, true, family, TcpTableOwnerPidAll, 0);
        if (first != 122 || size <= 4)
            yield break;

        var buffer = Marshal.AllocHGlobal(size);
        try
        {
            var result = GetExtendedTcpTable(buffer, ref size, true, family, TcpTableOwnerPidAll, 0);
            if (result != 0)
                yield break;

            var count = Marshal.ReadInt32(buffer);
            var rowSize = ipv6 ? Marshal.SizeOf<Tcp6RowOwnerPid>() : Marshal.SizeOf<TcpRowOwnerPid>();
            var rowPtr = IntPtr.Add(buffer, 4);

            for (var i = 0; i < count; i++)
            {
                var state = 0;
                var pid = 0;
                string local = "";
                string remote = "";

                if (ipv6)
                {
                    var row = Marshal.PtrToStructure<Tcp6RowOwnerPid>(rowPtr);
                    state = (int)row.State;
                    pid = (int)row.OwningPid;
                    local = FormatV6(row.LocalAddr, row.LocalPort);
                    remote = FormatV6(row.RemoteAddr, row.RemotePort);
                }
                else
                {
                    var row = Marshal.PtrToStructure<TcpRowOwnerPid>(rowPtr);
                    state = (int)row.State;
                    pid = (int)row.OwningPid;
                    local = FormatV4(row.LocalAddr, row.LocalPort);
                    remote = FormatV4(row.RemoteAddr, row.RemotePort);
                }

                if (state is 1 or 12)
                {
                    rowPtr = IntPtr.Add(rowPtr, rowSize);
                    continue;
                }

                if (!names.TryGetValue(pid, out var process))
                {
                    process = GetProcessName(pid);
                    names[pid] = process;
                }

                yield return new ConnectionEntry(
                    pid,
                    process,
                    local,
                    remote,
                    StateName(state),
                    state == 2);

                rowPtr = IntPtr.Add(rowPtr, rowSize);
            }
        }
        finally
        {
            Marshal.FreeHGlobal(buffer);
        }
    }

    private static string GetProcessName(int pid)
    {
        if (pid == 0)
            return "System";
        if (pid == 4)
            return "System";

        try
        {
            using var process = Process.GetProcessById(pid);
            return process.ProcessName;
        }
        catch
        {
            return $"PID {pid}";
        }
    }

    private static string FormatV4(uint address, uint port)
    {
        var bytes = BitConverter.GetBytes(address);
        var ip = new IPAddress(bytes);
        return $"{ip}:{HostPort(port)}";
    }

    private static string FormatV6(byte[] address, uint port)
    {
        try
        {
            var ip = new IPAddress(address);
            return $"[{ip}]:{HostPort(port)}";
        }
        catch
        {
            return $"?:{HostPort(port)}";
        }
    }

    private static ushort HostPort(uint port) =>
        (ushort)System.Net.IPAddress.NetworkToHostOrder((short)port);

    private static string StateName(int state) => state switch
    {
        2 => "LISTEN",
        3 => "SYN_SENT",
        4 => "SYN_RCVD",
        5 => "ESTABLISHED",
        6 => "FIN_WAIT1",
        7 => "FIN_WAIT2",
        8 => "CLOSE_WAIT",
        9 => "CLOSING",
        10 => "LAST_ACK",
        11 => "TIME_WAIT",
        _ => state.ToString()
    };
}
