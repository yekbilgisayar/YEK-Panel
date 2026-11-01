using System.IO;
using System.Text.Encodings.Web;
using System.Text.Json;

namespace YekPanel.Services;

public sealed class NotificationEntry
{
    public DateTime Utc { get; set; }
    public string Server { get; set; } = "";
    public bool Recovered { get; set; }
}

public static class NotificationLog
{
    private const int MaxEntries = 200;

    private static readonly string Dir =
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "YekPanel");

    private static readonly string FilePath = Path.Combine(Dir, "notifications.json");

    private static readonly JsonSerializerOptions Options = new()
    {
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    private static List<NotificationEntry>? _entries;

    private static List<NotificationEntry> Entries => _entries ??= Load();

    public static void Add(string server, bool recovered)
    {
        if (string.IsNullOrWhiteSpace(server))
            return;

        Entries.Add(new NotificationEntry
        {
            Utc = DateTime.UtcNow,
            Server = server,
            Recovered = recovered
        });

        if (Entries.Count > MaxEntries)
            Entries.RemoveRange(0, Entries.Count - MaxEntries);

        Save();
    }

    public static IReadOnlyList<NotificationEntry> Recent(int count) =>
        Entries.OrderByDescending(e => e.Utc).Take(count).ToList();

    public static int CountIssuesSince(DateTime utc)
    {
        try
        {
            return Entries.Count(e => e.Utc >= utc && !e.Recovered);
        }
        catch
        {
            return 0;
        }
    }

    public static void Clear()
    {
        _entries = new List<NotificationEntry>();
        try
        {
            if (File.Exists(FilePath))
                File.Delete(FilePath);
        }
        catch
        {
        }
    }

    private static List<NotificationEntry> Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                var loaded = JsonSerializer.Deserialize<List<NotificationEntry>>(
                    File.ReadAllText(FilePath), Options);
                if (loaded != null)
                    return loaded;
            }
        }
        catch
        {
        }

        return new List<NotificationEntry>();
    }

    private static void Save()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(Entries, Options));
        }
        catch
        {
        }
    }
}
