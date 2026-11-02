namespace YekPanel.Services;

public sealed record ClipboardEntry(string Text, DateTime Time);

/// <summary>Son kopyalanan metinleri yalnızca bellekte tutar; diske yazılmaz.</summary>
public sealed class ClipboardHistory
{
    public const int Capacity = 15;
    private const int MaxLength = 5000;

    private readonly List<ClipboardEntry> _items = new();
    private string _last = "";

    public IReadOnlyList<ClipboardEntry> Items => _items;

    public event Action? Changed;

    public void Capture(string? text)
    {
        if (string.IsNullOrWhiteSpace(text))
            return;

        var clean = text.Trim();
        if (clean.Length > MaxLength)
            clean = clean[..MaxLength];

        if (clean == _last)
            return;

        _last = clean;
        _items.RemoveAll(entry => entry.Text == clean);
        _items.Insert(0, new ClipboardEntry(clean, DateTime.Now));

        while (_items.Count > Capacity)
            _items.RemoveAt(_items.Count - 1);

        Changed?.Invoke();
    }

    public void Clear()
    {
        _items.Clear();
        _last = "";
        Changed?.Invoke();
    }

    public static string Preview(string text, int length = 70)
    {
        var line = text.Replace("\r", " ").Replace("\n", " ").Replace("\t", " ");
        while (line.Contains("  ", StringComparison.Ordinal))
            line = line.Replace("  ", " ", StringComparison.Ordinal);

        line = line.Trim();
        return line.Length <= length ? line : line[..(length - 1)] + "…";
    }
}
