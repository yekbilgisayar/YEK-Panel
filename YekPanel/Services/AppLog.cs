using System.IO;
using System.Text;
using YekPanel.Models;

namespace YekPanel.Services;

/// <summary>
/// Beklenmeyen hataları %AppData%\YekPanel\crash.log dosyasına yazar.
/// Dosya 256 KB'a ulaşınca crash.old.log olarak döndürülür.
/// </summary>
public static class AppLog
{
    private const long MaxBytes = 256 * 1024;
    private static readonly object Gate = new();

    public static string FilePath => Path.Combine(SettingsStore.DataDirectory, "crash.log");

    public static string OldFilePath => Path.Combine(SettingsStore.DataDirectory, "crash.old.log");

    public static bool HasEntries
    {
        get
        {
            try
            {
                return File.Exists(FilePath) && new FileInfo(FilePath).Length > 0;
            }
            catch
            {
                return false;
            }
        }
    }

    public static void Error(string source, Exception exception)
    {
        try
        {
            lock (Gate)
            {
                Directory.CreateDirectory(SettingsStore.DataDirectory);
                RotateIfNeeded();

                var text = new StringBuilder()
                    .Append('[').Append(DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")).Append("] ")
                    .AppendLine(source)
                    .AppendLine($"{exception.GetType().FullName}: {exception.Message}")
                    .AppendLine(exception.StackTrace ?? "")
                    .AppendLine()
                    .ToString();

                File.AppendAllText(FilePath, text);
            }
        }
        catch
        {
            // Günlük yazılamazsa uygulamanın akışı bozulmaz.
        }
    }

    private static void RotateIfNeeded()
    {
        if (!File.Exists(FilePath))
            return;

        if (new FileInfo(FilePath).Length < MaxBytes)
            return;

        try
        {
            File.Delete(OldFilePath);
        }
        catch
        {
        }

        File.Move(FilePath, OldFilePath);
    }
}
