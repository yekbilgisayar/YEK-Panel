using System.IO;
using System.Text.Encodings.Web;
using System.Text.Json;

namespace YekPanel.Models;

public sealed class EndpointConfig
{
    public string Name { get; set; } = "";
    public string Url { get; set; } = "";
    public string Mac { get; set; } = "";
    public bool Maintenance { get; set; }
}

public sealed class AppSettings
{
    public List<EndpointConfig> Endpoints { get; set; } = DefaultEndpoints();
    public int RefreshSeconds { get; set; } = 30;
    public bool Topmost { get; set; } = false;
    public bool StartWithWindows { get; set; } = false;
    public bool NotificationsEnabled { get; set; } = true;
    public bool CloseToTray { get; set; } = true;
    public bool QuietHoursEnabled { get; set; } = false;
    public string QuietStart { get; set; } = "22:00";
    public string QuietEnd { get; set; } = "08:00";
    public string Accent { get; set; } = "sky";
    public string City { get; set; } = "";
    public double? WindowLeft { get; set; }
    public double? WindowTop { get; set; }

    public double WindowOpacity { get; set; } = 1.0;
    public bool Clock24Hour { get; set; } = true;
    public bool ShowSeconds { get; set; } = true;
    public bool UseFahrenheit { get; set; } = false;
    public bool StartHidden { get; set; } = false;
    public bool RememberMini { get; set; } = false;
    public bool MiniMode { get; set; } = false;
    public int SlowThresholdMs { get; set; } = 1500;
    public bool DailySummaryEnabled { get; set; } = false;
    public string DailySummaryTime { get; set; } = "09:00";
    public string LastSummaryDate { get; set; } = "";
    public string QuickNote { get; set; } = "";

    public bool AutoUpdateCheck { get; set; } = true;
    public bool AutoInstallUpdates { get; set; } = true;
    public string LastUpdateCheck { get; set; } = "";
    public string SkippedVersion { get; set; } = "";
    public string LatestVersion { get; set; } = "";
    public string LatestReleaseUrl { get; set; } = "";
    public string LatestReleaseName { get; set; } = "";

    public bool CpuAlertEnabled { get; set; } = true;
    public int CpuAlertC { get; set; } = 85;
    public bool RamAlertEnabled { get; set; } = true;
    public int RamAlertPercent { get; set; } = 90;
    public bool SoundAlerts { get; set; } = false;

    public bool BridgeEnabled { get; set; } = true;
    public int BridgePort { get; set; } = 8787;
    public string BridgePin { get; set; } = "";

    public bool ClipboardHistoryEnabled { get; set; } = true;
    public string CardOrder { get; set; } = "computer,connection,server";
    public string HiddenCards { get; set; } = "";
    public List<ScriptEntry> Scripts { get; set; } = new();
    public List<ScheduledTask> ScheduledTasks { get; set; } = new();

    public static List<EndpointConfig> DefaultEndpoints() => new()
    {
        new EndpointConfig { Name = "Web Sitesi", Url = "" },
        new EndpointConfig { Name = "Sunucu Paneli", Url = "" },
        new EndpointConfig { Name = "Home Assistant", Url = "" },
        new EndpointConfig { Name = "Nextcloud", Url = "" }
    };
}

public static class SettingsStore
{
    private static readonly string Dir =
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "YekPanel");

    private static readonly string FilePath = Path.Combine(Dir, "settings.json");

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        WriteIndented = true,
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    public static string FilePathPublic => FilePath;

    public static string DataDirectory => Dir;

    public static bool ReplaceFromFile(string source)
    {
        try
        {
            var loaded = JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(source), JsonOptions);
            if (loaded == null)
                return false;

            Normalize(loaded);
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(loaded, JsonOptions));
            return true;
        }
        catch
        {
            return false;
        }
    }

    public static AppSettings Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                var loaded = JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(FilePath), JsonOptions);
                if (loaded != null)
                {
                    Normalize(loaded);
                    return loaded;
                }
            }
        }
        catch
        {
            // Bozuk ayar dosyası varsayılanlara döner.
        }

        return new AppSettings();
    }

    public static void Save(AppSettings settings)
    {
        try
        {
            Normalize(settings);
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(settings, JsonOptions));
        }
        catch
        {
            // Ayar yazılamazsa uygulama mevcut değerlerle çalışmaya devam eder.
        }
    }

    private static void Normalize(AppSettings settings)
    {
        settings.RefreshSeconds = Math.Clamp(settings.RefreshSeconds, 5, 3600);
        settings.City = (settings.City ?? "").Trim();
        settings.QuietStart = NormalizeClock(settings.QuietStart, "22:00");
        settings.QuietEnd = NormalizeClock(settings.QuietEnd, "08:00");
        settings.DailySummaryTime = NormalizeClock(settings.DailySummaryTime, "09:00");
        settings.Accent = settings.Accent is "teal" or "green" or "amber" ? settings.Accent : "sky";
        settings.WindowOpacity = Math.Clamp(settings.WindowOpacity, 0.6, 1.0);
        settings.SlowThresholdMs = Math.Clamp(settings.SlowThresholdMs, 100, 30000);
        settings.QuickNote = (settings.QuickNote ?? "").Trim();
        if (settings.QuickNote.Length > 2000)
            settings.QuickNote = settings.QuickNote[..2000];
        settings.LastSummaryDate = (settings.LastSummaryDate ?? "").Trim();
        settings.LastUpdateCheck = (settings.LastUpdateCheck ?? "").Trim();
        settings.SkippedVersion = Trim(settings.SkippedVersion, 32);
        settings.LatestVersion = Trim(settings.LatestVersion, 32);
        settings.LatestReleaseUrl = Trim(settings.LatestReleaseUrl, 500);
        settings.LatestReleaseName = Trim(settings.LatestReleaseName, 200);

        settings.CpuAlertC = Math.Clamp(settings.CpuAlertC, 50, 110);
        settings.RamAlertPercent = Math.Clamp(settings.RamAlertPercent, 50, 100);
        settings.BridgePort = Math.Clamp(settings.BridgePort, 1024, 65535);
        settings.BridgePin = (settings.BridgePin ?? "").Trim();
        if (settings.BridgePin.Length != 6 || !settings.BridgePin.All(char.IsDigit))
            settings.BridgePin = "";
        settings.CardOrder = NormalizeCardOrder(settings.CardOrder);
        settings.HiddenCards = NormalizeHiddenCards(settings.HiddenCards);
        NormalizeScripts(settings);
        NormalizeScheduledTasks(settings);

        var defaults = AppSettings.DefaultEndpoints();
        if (settings.Endpoints == null)
            settings.Endpoints = defaults;

        while (settings.Endpoints.Count < defaults.Count)
        {
            var fallback = defaults[settings.Endpoints.Count];
            settings.Endpoints.Add(new EndpointConfig { Name = fallback.Name, Url = fallback.Url });
        }

        if (settings.Endpoints.Count > defaults.Count)
            settings.Endpoints.RemoveRange(defaults.Count, settings.Endpoints.Count - defaults.Count);

        for (int i = 0; i < settings.Endpoints.Count; i++)
        {
            if (string.IsNullOrWhiteSpace(settings.Endpoints[i].Name))
                settings.Endpoints[i].Name = defaults[i].Name;
            settings.Endpoints[i].Url = NormalizeUrl(settings.Endpoints[i].Url);
            settings.Endpoints[i].Mac = (settings.Endpoints[i].Mac ?? "").Trim();
        }
    }

    private static string Trim(string? value, int maxLength)
    {
        var text = (value ?? "").Trim();
        return text.Length > maxLength ? text[..maxLength] : text;
    }

    private static readonly string[] CardKeys = { "computer", "connection", "server" };

    private static string NormalizeCardOrder(string? value)
    {
        var keys = (value ?? "")
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(key => key.ToLowerInvariant())
            .Where(key => CardKeys.Contains(key))
            .Distinct()
            .ToList();

        foreach (var key in CardKeys)
        {
            if (!keys.Contains(key))
                keys.Add(key);
        }

        return string.Join(",", keys);
    }

    private static string NormalizeHiddenCards(string? value)
    {
        var keys = (value ?? "")
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(key => key.ToLowerInvariant())
            .Where(key => CardKeys.Contains(key))
            .Distinct();

        return string.Join(",", keys);
    }

    private static void NormalizeScripts(AppSettings settings)
    {
        settings.Scripts ??= new List<ScriptEntry>();
        var list = settings.Scripts
            .Where(script => script != null)
            .Take(20)
            .ToList();

        foreach (var script in list)
        {
            script.Name = Trim(script.Name, 60);
            script.Path = Trim(script.Path, 500);
            script.Arguments = Trim(script.Arguments, 300);
            if (script.Name.Length == 0)
                script.Name = System.IO.Path.GetFileNameWithoutExtension(script.Path);
        }

        settings.Scripts = list;
    }

    private static void NormalizeScheduledTasks(AppSettings settings)
    {
        settings.ScheduledTasks ??= new List<ScheduledTask>();
        var list = settings.ScheduledTasks
            .Where(task => task != null)
            .Take(30)
            .ToList();

        foreach (var task in list)
        {
            task.Time = NormalizeClock(task.Time, "09:00");
            task.Action = ScheduledTask.Actions.Contains(task.Action) ? task.Action : "notify";
            task.Target = Trim(task.Target, 500);
            task.Name = Trim(task.Name, 60);
            if (task.Name.Length == 0)
                task.Name = $"{ScheduledTask.ActionLabel(task.Action)} {task.Time}";
            task.LastRunDate = Trim(task.LastRunDate, 10);
        }

        settings.ScheduledTasks = list;
    }

    private static string NormalizeClock(string? value, string fallback)
    {
        var text = (value ?? "").Trim();
        return TimeSpan.TryParseExact(text, @"hh\:mm", System.Globalization.CultureInfo.InvariantCulture, out var time)
            ? time.ToString(@"hh\:mm")
            : fallback;
    }

    private static string NormalizeUrl(string? url)
    {
        var value = (url ?? "").Trim();
        if (value.Length > 0 && value.IndexOf("://", StringComparison.Ordinal) < 0)
            value = "http://" + value;
        return value;
    }
}
