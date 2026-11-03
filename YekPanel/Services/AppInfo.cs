using System.Reflection;

namespace YekPanel.Services;

/// <summary>
/// Sürüm ve tanıtım bilgileri tek kaynaktan okunur (csproj &lt;Version&gt;).
/// </summary>
public static class AppInfo
{
    public const string RepoOwner = "yekbilgisayar";
    public const string RepoName = "YEK-Panel";
    public const int FeatureCount = 134;

    public static Version Version =>
        Assembly.GetExecutingAssembly().GetName().Version ?? new Version(2, 3, 1, 0);

    public static string VersionFull => $"{Version.Major}.{Version.Minor}.{Version.Build}";

    public static string VersionLabel => $"v{Version.Major}.{Version.Minor}";

    public static string ReleasesUrl => $"https://github.com/{RepoOwner}/{RepoName}/releases";

    public static string LatestReleaseApi =>
        $"https://api.github.com/repos/{RepoOwner}/{RepoName}/releases/latest";
}
