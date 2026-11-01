using System.Diagnostics;

namespace YekPanel.Services;

public sealed record ProcessSample(string Name, double CpuPercent, double MemoryMb);

public sealed class ProcessMonitor
{
    private readonly Dictionary<int, TimeSpan> _lastCpu = new();
    private DateTime _lastSample = DateTime.UtcNow;

    public IReadOnlyList<ProcessSample> TopByMemory(int count)
    {
        var now = DateTime.UtcNow;
        var elapsedMs = Math.Max(200, (now - _lastSample).TotalMilliseconds);
        var cores = Math.Max(1, Environment.ProcessorCount);

        var groups = new Dictionary<string, (double Cpu, double Memory)>(StringComparer.OrdinalIgnoreCase);
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
                    groups[process.ProcessName] = (existing.Cpu + cpu, existing.Memory + memoryMb);
                else
                    groups[process.ProcessName] = (cpu, memoryMb);
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
                pair.Value.Memory))
            .OrderByDescending(s => s.MemoryMb)
            .Take(count)
            .ToList();
    }
}
