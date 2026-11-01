using System.Net.Http;
using System.Text.Json;

namespace YekPanel.Services;

public sealed record ReleaseInfo(Version Version, string Tag, string Name, string PageUrl, string Notes);

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

    public static async Task<ReleaseInfo?> GetLatestAsync(CancellationToken token = default)
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
                return null;

            await using var stream = await response.Content.ReadAsStreamAsync(token);
            using var document = await JsonDocument.ParseAsync(stream, cancellationToken: token);
            var root = document.RootElement;

            var tag = root.TryGetProperty("tag_name", out var tagElement)
                ? tagElement.GetString() ?? ""
                : "";

            if (!TryParseVersion(tag, out var version))
                return null;

            var name = root.TryGetProperty("name", out var nameElement)
                ? nameElement.GetString() ?? tag
                : tag;

            var page = root.TryGetProperty("html_url", out var pageElement)
                ? pageElement.GetString() ?? ""
                : "";

            var notes = root.TryGetProperty("body", out var bodyElement)
                ? bodyElement.GetString() ?? ""
                : "";

            return new ReleaseInfo(version, tag, name, page, notes);
        }
        catch
        {
            return null;
        }
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
