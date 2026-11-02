using System.Globalization;
using YekPanel.Models;

namespace YekPanel.Services;

public sealed record ScheduleResult(bool Alert, string Message);

/// <summary>Günde bir kez çalışan görevlerin zaman kontrolü ve yürütmesi.</summary>
public static class SchedulerService
{
    public static IReadOnlyList<ScheduledTask> Due(AppSettings settings, DateTime now)
    {
        var today = now.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        var due = new List<ScheduledTask>();

        foreach (var task in settings.ScheduledTasks)
        {
            if (!task.Enabled || task.LastRunDate == today)
                continue;

            if (!TimeSpan.TryParseExact(task.Time, @"hh\:mm", CultureInfo.InvariantCulture, out var target))
                continue;

            if (now.TimeOfDay >= target)
                due.Add(task);
        }

        return due;
    }

    public static async Task<ScheduleResult> ExecuteAsync(ScheduledTask task, AppSettings settings)
    {
        switch (task.Action)
        {
            case "script":
            {
                var script = settings.Scripts.FirstOrDefault(entry =>
                                 string.Equals(entry.Path, task.Target, StringComparison.OrdinalIgnoreCase)) ??
                             settings.Scripts.FirstOrDefault(entry =>
                                 string.Equals(entry.Name, task.Target, StringComparison.OrdinalIgnoreCase));

                var entry = script ?? new ScriptEntry { Name = task.Name, Path = task.Target };
                var (started, message) = ScriptRunner.Run(entry);
                return new ScheduleResult(!started, $"{task.Name}: {message}");
            }
            case "ping":
            {
                if (string.IsNullOrWhiteSpace(task.Target))
                    return new ScheduleResult(true, $"{task.Name}: hedef boş");

                var latency = await NetworkMonitor.PingAsync(task.Target.Trim());
                return latency is long ms
                    ? new ScheduleResult(false, $"{task.Name}: {task.Target} yanıt verdi ({ms} ms)")
                    : new ScheduleResult(true, $"{task.Name}: {task.Target} yanıt vermiyor");
            }
            case "kill":
            {
                if (string.IsNullOrWhiteSpace(task.Target))
                    return new ScheduleResult(true, $"{task.Name}: uygulama adı boş");

                var (killed, error) = ProcessManager.KillGroup(task.Target.Trim());
                return killed > 0
                    ? new ScheduleResult(false, $"{task.Name}: {killed} süreç kapatıldı")
                    : new ScheduleResult(true, $"{task.Name}: {error}");
            }
            default:
                return new ScheduleResult(!string.IsNullOrWhiteSpace(task.Target),
                    $"{task.Name}: {task.Target}");
        }
    }
}
