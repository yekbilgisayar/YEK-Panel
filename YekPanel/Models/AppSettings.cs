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
    public string LastUpdateCheck { get; set; } = "";
    public string SkippedVersion { get; set; } = "";
    public string LatestVersion { get; set; } = "";
    public string LatestReleaseUrl { get; set; } = "";
    public string LatestReleaseName { get; set; } = "";

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
