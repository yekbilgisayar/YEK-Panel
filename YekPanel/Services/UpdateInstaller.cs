using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Security.Cryptography;

namespace YekPanel.Services;

/// <summary>
/// Kurulum paketini indirir, yayındaki SHA-256 ile doğrular ve Inno Setup'ı
/// sessiz modda çalıştırır.
/// </summary>
public static class UpdateInstaller
{
    private const string InstallerArguments = "/SILENT /NORESTART /CLOSEAPPLICATIONS /SP-";

    private static readonly HttpClient Http = new(new SocketsHttpHandler
    {
        ConnectTimeout = TimeSpan.FromSeconds(15),
        AutomaticDecompression = System.Net.DecompressionMethods.All
    })
    {
        Timeout = TimeSpan.FromMinutes(10)
    };

    public static string UpdatesDir => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "YekPanel",
        "Updates");

    public static string TargetPath(ReleaseInfo release) => Path.Combine(
        UpdatesDir,
        $"YEK-Panel-Setup-{release.Version.Major}.{release.Version.Minor}.{release.Version.Build}.exe");

    public static void CleanupOld()
    {
        try
        {
            if (!Directory.Exists(UpdatesDir))
                return;

            var limit = DateTime.UtcNow.AddDays(-14);
            foreach (var file in Directory.EnumerateFiles(UpdatesDir))
            {
                if (File.GetLastWriteTimeUtc(file) < limit)
                    File.Delete(file);
            }
        }
        catch
        {
            // Temizlik yapılamazsa indirme akışı etkilenmez.
        }
    }

    public static async Task<string> DownloadAsync(
        ReleaseInfo release,
        IProgress<double>? progress,
        CancellationToken token)
    {
        if (string.IsNullOrWhiteSpace(release.SetupUrl))
            throw new InvalidOperationException("Kurulum paketi adresi yok.");

        Directory.CreateDirectory(UpdatesDir);
        var target = TargetPath(release);
        var partial = target + ".part";

        TryDelete(target);
        TryDelete(partial);

        using var request = new HttpRequestMessage(HttpMethod.Get, release.SetupUrl);
        request.Headers.UserAgent.ParseAdd($"YekPanel/{AppInfo.VersionFull}");

        using var response = await Http.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, token);
        response.EnsureSuccessStatusCode();

        var total = response.Content.Headers.ContentLength ?? release.SetupSize;
        await using var source = await response.Content.ReadAsStreamAsync(token);
        await using (var destination = new FileStream(
                         partial, FileMode.CreateNew, FileAccess.Write, FileShare.None, 81920, true))
        {
            var buffer = new byte[81920];
            long received = 0;
            int count;

            while ((count = await source.ReadAsync(buffer, token)) > 0)
            {
                await destination.WriteAsync(buffer.AsMemory(0, count), token);
                received += count;

                if (total > 0)
                    progress?.Report(Math.Min(100d, received * 100d / total));
            }
        }

        File.Move(partial, target, overwrite: true);
        progress?.Report(100);
        return target;
    }

    public static bool Verify(string path, ReleaseInfo release, out string error)
    {
        error = "";

        if (!File.Exists(path))
        {
            error = "dosya bulunamadı";
            return false;
        }

        var info = new FileInfo(path);
        if (info.Length <= 0)
        {
            error = "dosya boş";
            return false;
        }

        if (release.SetupSize > 0 && info.Length != release.SetupSize)
        {
            error = $"boyut uyuşmuyor ({info.Length} / {release.SetupSize} bayt)";
            return false;
        }

        if (string.IsNullOrWhiteSpace(release.SetupSha256))
        {
            error = "yayında SHA-256 özeti yok";
            return false;
        }

        string actual;
        using (var stream = File.OpenRead(path))
            actual = Convert.ToHexString(SHA256.HashData(stream)).ToLowerInvariant();

        if (!string.Equals(actual, release.SetupSha256, StringComparison.OrdinalIgnoreCase))
        {
            error = "SHA-256 özeti uyuşmuyor";
            return false;
        }

        try
        {
            var product = FileVersionInfo.GetVersionInfo(path).ProductName?.Trim();
            if (!string.Equals(product, "YEK Panel", StringComparison.OrdinalIgnoreCase))
            {
                error = "paket YEK Panel kurulumu değil";
                return false;
            }
        }
        catch (Exception)
        {
            error = "paket bilgisi okunamadı";
            return false;
        }

        return true;
    }

    public static bool TryLaunch(string path, out string error)
    {
        error = "";

        try
        {
            var full = Path.GetFullPath(path);
            var root = Path.GetFullPath(UpdatesDir) + Path.DirectorySeparatorChar;
            if (!full.StartsWith(root, StringComparison.OrdinalIgnoreCase))
            {
                error = "beklenmeyen dosya yolu";
                return false;
            }

            Process.Start(new ProcessStartInfo
            {
                FileName = full,
                Arguments = InstallerArguments,
                UseShellExecute = true,
                WorkingDirectory = root
            });
            return true;
        }
        catch (Exception exception)
        {
            error = exception.Message;
            return false;
        }
    }

    public static void TryDelete(string path)
    {
        try
        {
            if (File.Exists(path))
                File.Delete(path);
        }
        catch
        {
            // Kilitli dosya sonraki temizlikte silinir.
        }
    }
}
