using System.IO;
using System.Runtime.InteropServices;

namespace YekPanel.Services;

public sealed class SystemSnapshot
{
    public double CpuPercent { get; init; }
    public ulong TotalRamBytes { get; init; }
    public ulong AvailableRamBytes { get; init; }
    public ulong DiskTotalBytes { get; init; }
    public ulong DiskFreeBytes { get; init; }
    public TimeSpan Uptime { get; init; }
    public string MachineName { get; init; } = "";
}

public sealed class SystemMonitor
{
    [StructLayout(LayoutKind.Sequential)]
    private struct FileTime
    {
        public uint Low;
        public uint High;
        public readonly ulong Value => ((ulong)High << 32) | Low;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MemoryStatusEx
    {
        public uint Length;
        public uint MemoryLoad;
        public ulong TotalPhys;
        public ulong AvailPhys;
        public ulong TotalPageFile;
        public ulong AvailPageFile;
        public ulong TotalVirtual;
        public ulong AvailVirtual;
        public ulong AvailExtendedVirtual;
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool GetSystemTimes(out FileTime idleTime, out FileTime kernelTime, out FileTime userTime);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool GlobalMemoryStatusEx(ref MemoryStatusEx buffer);

    private ulong _prevIdle;
    private ulong _prevKernel;
    private ulong _prevUser;
    private bool _hasSample;

    public SystemSnapshot Read()
    {
        double cpu = 0;

        if (GetSystemTimes(out var idle, out var kernel, out var user))
        {
            var idleNow = idle.Value;
            var kernelNow = kernel.Value;
            var userNow = user.Value;

            if (_hasSample)
            {
                var idleDelta = idleNow - _prevIdle;
                var totalDelta = (kernelNow - _prevKernel) + (userNow - _prevUser);
                if (totalDelta > 0)
                    cpu = Math.Clamp((1.0 - (double)idleDelta / totalDelta) * 100.0, 0, 100);
            }

            _prevIdle = idleNow;
            _prevKernel = kernelNow;
            _prevUser = userNow;
            _hasSample = true;
        }

        ulong totalRam = 0;
        ulong freeRam = 0;
        var mem = new MemoryStatusEx { Length = (uint)Marshal.SizeOf<MemoryStatusEx>() };
        if (GlobalMemoryStatusEx(ref mem))
        {
            totalRam = mem.TotalPhys;
            freeRam = mem.AvailPhys;
        }

        ulong diskTotal = 0;
        ulong diskFree = 0;
        try
        {
            var root = Path.GetPathRoot(Environment.SystemDirectory) ?? "C:\\";
            var drive = new DriveInfo(root);
            diskTotal = (ulong)drive.TotalSize;
            diskFree = (ulong)drive.AvailableFreeSpace;
        }
        catch
        {
            // Sürücü bilgisi okunamazsa 0 olarak kalır.
        }

        return new SystemSnapshot
        {
            CpuPercent = cpu,
            TotalRamBytes = totalRam,
            AvailableRamBytes = freeRam,
            DiskTotalBytes = diskTotal,
            DiskFreeBytes = diskFree,
            Uptime = TimeSpan.FromMilliseconds(Environment.TickCount64),
            MachineName = Environment.MachineName
        };
    }
}
