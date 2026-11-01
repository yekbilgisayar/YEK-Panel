using System.IO;
using System.Text.Encodings.Web;
using System.Text.Json;

namespace YekPanel.Services;

public sealed class UptimePoint
{
    public DateTime Utc { get; set; }
    public int State { get; set; }
}

public sealed record DayUptime(DateTime Day, double? Percent);

public sealed record Incident(string Server, DateTime StartLocal, TimeSpan Duration, bool Ongoing);

public static class UptimeHistory
{
    private const int Unknown = 0;
    private const int MinimumKnownSeconds = 1800;
    private const int MinimumDayKnownSeconds = 300;

    private static readonly string Dir =
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "YekPanel");

    private static readonly string FilePath = Path.Combine(Dir, "history.json");

    private static readonly JsonSerializerOptions Options = new()
    {
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    private static Dictionary<string, List<UptimePoint>>? _data;

    private static Dictionary<string, List<UptimePoint>> Data => _data ??= Load();

    public static void Record(string key, int state)
    {
        if (string.IsNullOrWhiteSpace(key) || state <= 0)
            return;

        var list = GetList(key);
        var now = DateTime.UtcNow;

        if (list.Count > 0)
        {
            var last = list[^1];
            if (now - last.Utc > TimeSpan.FromMinutes(10) && last.State != Unknown)
                list.Add(new UptimePoint { Utc = last.Utc.AddSeconds(1), State = Unknown });
        }

        if (list.Count == 0 || list[^1].State != state)
        {
            list.Add(new UptimePoint { Utc = now, State = state });
            Trim(list, now);
            Save();
        }
    }

    public static double? Percent(string key, TimeSpan window)
    {
        if (!Data.TryGetValue(key, out var list) || list.Count == 0)
            return null;

        var now = DateTime.UtcNow;
        if (now - list[^1].Utc > TimeSpan.FromHours(48))
            return null;

        return PercentBetween(list, now - window, now, MinimumKnownSeconds);
    }

    /// <summary>Son günlerin günlük çevrimiçi oranları. Veri olmayan günlerde Percent null döner.</summary>
    public static IReadOnlyList<DayUptime> Daily(string key, int days)
    {
        var result = new List<DayUptime>(days);
        var today = DateTime.Now.Date;
        var now = DateTime.UtcNow;

        if (!Data.TryGetValue(key, out var list))
            list = new List<UptimePoint>();

        for (int offset = days - 1; offset >= 0; offset--)
        {
            var day = today.AddDays(-offset);
            var start = day.ToUniversalTime();
            var end = day.AddDays(1).ToUniversalTime();
            if (end > now)
                end = now;

            var percent = end > start
                ? PercentBetween(list, start, end, MinimumDayKnownSeconds)
                : null;

            result.Add(new DayUptime(day, percent));
        }

        return result;
    }

    private static double? PercentBetween(List<UptimePoint> list, DateTime start, DateTime end, int minimumKnownSeconds)
    {
        if (list.Count == 0 || end <= start)
            return null;

        var index = -1;
        for (int i = 0; i < list.Count; i++)
        {
            if (list[i].Utc <= start)
                index = i;
            else
                break;
        }

        var state = index >= 0 ? list[index].State : Unknown;
        var cursor = start;
        double online = 0;
        double known = 0;

        for (int i = index + 1; i < list.Count; i++)
        {
            var point = list[i];
            if (point.Utc >= end)
                break;

            Accumulate(point.Utc - cursor, state, ref online, ref known);
            cursor = point.Utc;
            state = point.State;
        }

        Accumulate(end - cursor, state, ref online, ref known);

        if (known < minimumKnownSeconds)
            return null;

        return Math.Clamp(online / known * 100, 0, 100);
    }

    private static void Accumulate(TimeSpan span, int state, ref double online, ref double known)
    {
        var seconds = span.TotalSeconds;
        if (seconds <= 0)
            return;

        if (state == 1)
            online += seconds;
        if (state is 1 or 2 or 3)
            known += seconds;
    }

    /// <summary>Son günlerdeki çevrimdışı/hatalı aralıklar. En yeni önce.</summary>
    public static IReadOnlyList<Incident> Incidents(int days, int max = 12)
    {
        var now = DateTime.UtcNow;
        var windowStart = now.AddDays(-Math.Clamp(days, 1, 60));
        var result = new List<Incident>();

        foreach (var pair in Data)
        {
            var list = pair.Value;
            if (list.Count == 0)
                continue;

            var index = -1;
            for (int i = 0; i < list.Count; i++)
            {
                if (list[i].Utc <= windowStart)
                    index = i;
                else
                    break;
            }

            var state = index >= 0 ? list[index].State : Unknown;
            DateTime? openStart = state >= 2 ? windowStart : null;
            var cursor = windowStart;

            for (int i = index + 1; i < list.Count; i++)
            {
                var point = list[i];
                if (point.Utc > now)
                    break;

                if (state >= 2 && point.State < 2)
                {
                    var start = openStart ?? cursor;
                    AddIncident(result, pair.Key, start, point.Utc - start, ongoing: false);
                    openStart = null;
                }
                else if (state < 2 && point.State >= 2)
                {
                    openStart = point.Utc;
                }

                cursor = point.Utc;
                state = point.State;
            }

            if (state >= 2 && openStart is DateTime open)
                AddIncident(result, pair.Key, open, now - open, ongoing: true);
        }

        return result
            .OrderByDescending(i => i.StartLocal)
            .Take(max)
            .ToList();
    }

    private static void AddIncident(List<Incident> target, string server, DateTime startUtc, TimeSpan duration,
        bool ongoing)
    {
        if (duration < TimeSpan.FromSeconds(30))
            return;

        target.Add(new Incident(server, startUtc.ToLocalTime(), duration, ongoing));
    }

    public static void Clear()
    {
        _data = new Dictionary<string, List<UptimePoint>>();
        try
        {
            if (File.Exists(FilePath))
                File.Delete(FilePath);
        }
        catch
        {
        }
    }

    private static List<UptimePoint> GetList(string key)
    {
        if (!Data.TryGetValue(key, out var list))
        {
            list = new List<UptimePoint>();
            Data[key] = list;
        }

        return list;
    }

    private static void Trim(List<UptimePoint> list, DateTime now)
    {
        var cutoff = now.AddDays(-31);
        var keepFrom = 0;

        for (int i = 0; i < list.Count; i++)
        {
            if (list[i].Utc < cutoff)
                keepFrom = i;
            else
                break;
        }

        if (keepFrom > 0)
            list.RemoveRange(0, keepFrom);
    }

    private static Dictionary<string, List<UptimePoint>> Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                var loaded = JsonSerializer.Deserialize<Dictionary<string, List<UptimePoint>>>(
                    File.ReadAllText(FilePath), Options);
                if (loaded != null)
                    return loaded;
            }
        }
        catch
        {
        }

        return new Dictionary<string, List<UptimePoint>>();
    }

    private static void Save()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(Data, Options));
        }
        catch
        {
        }
    }
}
