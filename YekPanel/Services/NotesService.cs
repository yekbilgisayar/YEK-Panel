using System.IO;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;

namespace YekPanel.Services;

public sealed class NoteItem
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");
    public string Title { get; set; } = "Yeni not";
    public string Content { get; set; } = "";
    public DateTime Updated { get; set; } = DateTime.Now;
}

/// <summary>Çoklu hızlı notlar; %AppData%\YekPanel\notes.json içinde tutulur.</summary>
public static class NotesService
{
    private const int MaxNotes = 50;
    private const int MaxContent = 100_000;

    private static readonly string Dir =
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "YekPanel");

    private static readonly string FilePath = Path.Combine(Dir, "notes.json");

    private static readonly JsonSerializerOptions Options = new()
    {
        WriteIndented = true,
        Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    private static List<NoteItem>? _notes;

    public static IReadOnlyList<NoteItem> All => Notes;

    public static NoteItem? Get(string? id) =>
        Notes.FirstOrDefault(note => note.Id == id);

    public static NoteItem Create(string title = "Yeni not")
    {
        var note = new NoteItem
        {
            Title = string.IsNullOrWhiteSpace(title) ? "Yeni not" : title.Trim(),
            Updated = DateTime.Now
        };

        Notes.Insert(0, note);
        while (Notes.Count > MaxNotes)
            Notes.RemoveAt(Notes.Count - 1);

        Save();
        return note;
    }

    public static void Update(NoteItem note, string title, string content)
    {
        note.Title = string.IsNullOrWhiteSpace(title) ? "Adsız not" : title.Trim();
        if (note.Title.Length > 80)
            note.Title = note.Title[..80];

        note.Content = content.Length > MaxContent ? content[..MaxContent] : content;
        note.Updated = DateTime.Now;
        Save();
    }

    public static void Delete(string id)
    {
        Notes.RemoveAll(note => note.Id == id);
        Save();
    }

    public static void EnsureMigrated(string quickNote)
    {
        if (Notes.Count > 0 || string.IsNullOrWhiteSpace(quickNote))
            return;

        Notes.Add(new NoteItem
        {
            Title = "Hızlı not",
            Content = quickNote.Trim(),
            Updated = DateTime.Now
        });
        Save();
    }

    public static string Export(NoteItem note, bool markdown, string targetPath)
    {
        var builder = new StringBuilder();
        if (markdown)
        {
            builder.AppendLine($"# {note.Title}");
            builder.AppendLine();
            builder.AppendLine(note.Content);
        }
        else
        {
            builder.AppendLine(note.Title);
            builder.AppendLine(new string('-', Math.Min(40, Math.Max(8, note.Title.Length))));
            builder.AppendLine(note.Content);
        }

        File.WriteAllText(targetPath, builder.ToString(), new UTF8Encoding(false));
        return targetPath;
    }

    private static List<NoteItem> Notes => _notes ??= Load();

    private static List<NoteItem> Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                var loaded = JsonSerializer.Deserialize<List<NoteItem>>(
                    File.ReadAllText(FilePath), Options);
                if (loaded != null)
                {
                    foreach (var note in loaded.Where(note => note != null))
                    {
                        if (string.IsNullOrWhiteSpace(note.Id))
                            note.Id = Guid.NewGuid().ToString("N");
                        if (note.Title.Length > 80)
                            note.Title = note.Title[..80];
                        if (note.Content.Length > MaxContent)
                            note.Content = note.Content[..MaxContent];
                    }

                    return loaded.Take(MaxNotes).ToList();
                }
            }
        }
        catch
        {
        }

        return new List<NoteItem>();
    }

    private static void Save()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(Notes, Options));
        }
        catch (Exception ex)
        {
            AppLog.Error("NotesService", ex);
        }
    }
}
