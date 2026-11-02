using System.Diagnostics;

namespace YekPanel.Services;

public sealed record ProcessSample(
    string Name,
    double CpuPercent,
    double MemoryMb,
    int ProcessId = 0,
    int Count = 1);

public sealed class ProcessMonitor
{
    private readonly Dictionary<int, TimeSpan> _lastCpu = new();
    private DateTime _lastSample = DateTime.UtcNow;

    public IReadOnlyList<ProcessSample> TopByMemory(int count)
    {
        var now = DateTime.UtcNow;
        var elapsedMs = Math.Max(200, (now - _lastSample).TotalMilliseconds);
        var cores = Math.Max(1, Environment.ProcessorCount);

        var groups = new Dictionary<string, (double Cpu, double Memory, int Pid, int Count, double PidMemory)>(
            StringComparer.OrdinalIgnoreCase);
        var next = new Dictionary<int, TimeSpan>(256);

        foreach (var process in Process.GetProcesses())
        {
            try
            {
                var cpuTime = process.TotalProcessorTime;
                next[process.Id] = cpuTime;

                double cpu = 0;
                if (_lastCpu.TryGetValue(process.Id, out var previous))
                    cpu = Math.Clamp((cpuTime - previous).TotalMilliseconds / (elapsedMs * cores) * 100, 0, 100);

                var memoryMb = process.WorkingSet64 / 1024d / 1024d;
                if (groups.TryGetValue(process.ProcessName, out var existing))
                {
                    groups[process.ProcessName] = (
                        existing.Cpu + cpu,
                        existing.Memory + memoryMb,
                        memoryMb > existing.PidMemory ? process.Id : existing.Pid,
                        existing.Count + 1,
                        Math.Max(existing.PidMemory, memoryMb));
                }
                else
                {
                    groups[process.ProcessName] = (cpu, memoryMb, process.Id, 1, memoryMb);
                }
            }
            catch
            {
            }
            finally
            {
                process.Dispose();
            }
        }

        _lastCpu.Clear();
        foreach (var entry in next)
            _lastCpu[entry.Key] = entry.Value;
        _lastSample = now;

        return groups
            .Select(pair => new ProcessSample(
                pair.Key,
                Math.Min(100, pair.Value.Cpu),
                pair.Value.Memory,
                pair.Value.Pid,
                pair.Value.Count))
            .OrderByDescending(s => s.MemoryMb)
            .Take(count)
            .ToList();
    }
}

public static class ProcessManager
{
    private static readonly HashSet<string> Protected = new(StringComparer.OrdinalIgnoreCase)
    {
        "system", "idle", "registry", "memory compression", "secure system",
        "csrss", "wininit", "winlogon", "services", "lsass", "smss", "svchost",
        "dwm", "fontdrvhost", "audiodg", "sihost", "ctfmon"
    };

    public static bool IsProtected(string name) => Protected.Contains(name);

    public static (int Killed, string? Error) KillGroup(string name)
    {
        if (string.IsNullOrWhiteSpace(name))
            return (0, "Süreç adı boş");

        if (IsProtected(name))
            return (0, $"{name} korunan bir sistem süreci; kapatılmadı.");

        Process[] processes;
        try
        {
            processes = Process.GetProcessesByName(name);
        }
        catch (Exception ex)
        {
            return (0, ex.Message);
        }

        var killed = 0;
        string? error = null;

        foreach (var process in processes)
        {
            try
            {
                if (process.Id == Environment.ProcessId)
                {
                    error = "YEK Panel kendini kapatamaz.";
                    continue;
                }

                process.Kill(entireProcessTree: false);
                killed++;
            }
            catch (Exception ex)
            {
                error ??= ex is UnauthorizedAccessException or System.ComponentModel.Win32Exception
                    ? "Erişim reddedildi; yönetici olarak yeniden başlatmayı deneyin."
                    : ex.Message;
            }
            finally
            {
                process.Dispose();
            }
        }

        if (killed == 0 && error == null)
            error = "Süreç zaten kapanmış.";

        return (killed, error);
    }
}
