using System.Net.Http;
using System.Text.Json;

namespace YekPanel.Services;

public sealed record ReleaseInfo(
    Version Version,
    string Tag,
    string Name,
    string PageUrl,
    string Notes,
    string SetupUrl = "",
    long SetupSize = 0,
    string SetupSha256 = "")
{
    /// <summary>Otomatik kurulum için paket adresi ve özeti birlikte gerekir.</summary>
    public bool HasInstaller =>
        !string.IsNullOrWhiteSpace(SetupUrl) && !string.IsNullOrWhiteSpace(SetupSha256);
}

/// <summary>
/// GitHub Releases üzerinden en son sürümü okur. Test için YEK_UPDATE_FEED
/// ortam değişkeniyle JSON adresi değiştirilebilir.
/// </summary>
public static class UpdateService
{
    private static readonly HttpClient Http = new()
    {
        Timeout = TimeSpan.FromSeconds(8)
    };

    public static async Task<ReleaseInfo?> GetLatestAsync(CancellationToken token = default) =>
        (await GetLatestDetailedAsync(token)).Release;

    public static async Task<(ReleaseInfo? Release, string Error)> GetLatestDetailedAsync(
        CancellationToken token = default)
    {
        var url = Environment.GetEnvironmentVariable("YEK_UPDATE_FEED");
        if (string.IsNullOrWhiteSpace(url))
            url = AppInfo.LatestReleaseApi;

        try
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, url.Trim());
            request.Headers.UserAgent.ParseAdd($"YekPanel/{AppInfo.VersionFull}");
            request.Headers.Accept.ParseAdd("application/vnd.github+json");

            using var response = await Http.SendAsync(request, token);
            if (!response.IsSuccessStatusCode)
            {
                return (null, response.StatusCode switch
                {
                    System.Net.HttpStatusCode.Forbidden =>
                        "GitHub isteği sınırlandı (HTTP 403). Bir süre sonra yeniden deneyin.",
                    System.Net.HttpStatusCode.NotFound =>
                        "Sürüm bilgisi bulunamadı (HTTP 404).",
                    _ => $"Güncelleme sunucusu yanıtı: HTTP {(int)response.StatusCode}."
                });
            }

            await using var stream = await response.Content.ReadAsStreamAsync(token);
            using var document = await JsonDocument.ParseAsync(stream, cancellationToken: token);
            var root = document.RootElement;

            if (!TrySelectDesktopRelease(root, out var selected))
                return (null, "Masaüstü sürümü bulunamadı.");

            var tag = selected.TryGetProperty("tag_name", out var tagElement)
                ? tagElement.GetString() ?? ""
                : "";

            if (!TryParseVersion(tag, out var version))
                return (null, "Sürüm etiketi okunamadı.");

            var name = selected.TryGetProperty("name", out var nameElement)
                ? nameElement.GetString() ?? tag
                : tag;

            var page = selected.TryGetProperty("html_url", out var pageElement)
                ? pageElement.GetString() ?? ""
                : "";

            var notes = selected.TryGetProperty("body", out var bodyElement)
                ? bodyElement.GetString() ?? ""
                : "";

            var (setupUrl, setupSize, setupSha) = ReadSetupAsset(selected);

            return (new ReleaseInfo(version, tag, name, page, notes, setupUrl, setupSize, setupSha), "");
        }
        catch (Exception)
        {
            return (null, "Güncelleme sunucusuna ulaşılamadı. Bağlantıyı kontrol edip yeniden deneyin.");
        }
    }

    /// <summary>
    /// Yayın listesi, en yeniden eskiye gelir. Android yayınları ayrı
    /// etiketlendiği için masaüstü sürümü seçilirken atlanır.
    /// </summary>
    private static bool TrySelectDesktopRelease(JsonElement root, out JsonElement release)
    {
        release = default;

        if (root.ValueKind != JsonValueKind.Array)
        {
            release = root;
            return root.ValueKind == JsonValueKind.Object;
        }

        foreach (var item in root.EnumerateArray())
        {
            if (item.ValueKind != JsonValueKind.Object)
                continue;

            if (item.TryGetProperty("draft", out var draft) && draft.ValueKind == JsonValueKind.True)
                continue;

            if (item.TryGetProperty("prerelease", out var prerelease) &&
                prerelease.ValueKind == JsonValueKind.True)
                continue;

            var tag = item.TryGetProperty("tag_name", out var tagElement)
                ? tagElement.GetString() ?? ""
                : "";

            if (tag.StartsWith("android-", StringComparison.OrdinalIgnoreCase))
                continue;

            if (!TryParseVersion(tag, out _))
                continue;

            release = item;
            return true;
        }

        return false;
    }

    private static (string Url, long Size, string Sha256) ReadSetupAsset(JsonElement root)
    {
        if (!root.TryGetProperty("assets", out var assets) || assets.ValueKind != JsonValueKind.Array)
            return ("", 0, "");

        foreach (var asset in assets.EnumerateArray())
        {
            var name = asset.TryGetProperty("name", out var nameElement)
                ? nameElement.GetString() ?? ""
                : "";

            if (!name.EndsWith(".exe", StringComparison.OrdinalIgnoreCase) ||
                !name.Contains("Setup", StringComparison.OrdinalIgnoreCase))
                continue;

            var url = asset.TryGetProperty("browser_download_url", out var urlElement)
                ? urlElement.GetString() ?? ""
                : "";

            var size = asset.TryGetProperty("size", out var sizeElement) &&
                       sizeElement.TryGetInt64(out var parsedSize)
                ? parsedSize
                : 0;

            var digest = asset.TryGetProperty("digest", out var digestElement)
                ? digestElement.GetString() ?? ""
                : "";

            const string prefix = "sha256:";
            var sha = digest.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)
                ? digest[prefix.Length..].Trim()
                : "";

            if (!string.IsNullOrWhiteSpace(url) && sha.Length == 64)
                return (url, size, sha.ToLowerInvariant());
        }

        return ("", 0, "");
    }

    public static bool IsNewer(Version candidate) => Normalize(candidate) > Normalize(AppInfo.Version);

    public static bool TryParseVersion(string? tag, out Version version)
    {
        version = new Version(0, 0, 0);
        var text = (tag ?? "").Trim().TrimStart('v', 'V');

        var cut = text.IndexOfAny(new[] { '-', '+', ' ' });
        if (cut > 0)
            text = text[..cut];

        if (!Version.TryParse(text, out var parsed))
            return false;

        version = Normalize(parsed);
        return version.Major > 0;
    }

    private static Version Normalize(Version value) =>
        new(value.Major, value.Minor, Math.Max(value.Build, 0));
}
