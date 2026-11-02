using System.Diagnostics;
using System.IO;
using YekPanel.Models;

namespace YekPanel.Services;

/// <summary>Panelden betik, toplu iş dosyası veya program çalıştırır.</summary>
public static class ScriptRunner
{
    private static readonly HashSet<string> Supported = new(StringComparer.OrdinalIgnoreCase)
    {
        ".ps1", ".cmd", ".bat", ".py", ".exe"
    };

    public static bool IsSupported(string path) =>
        Supported.Contains(System.IO.Path.GetExtension(path ?? ""));

    public static (bool Started, string Message) Run(ScriptEntry script)
    {
        if (script == null || string.IsNullOrWhiteSpace(script.Path))
            return (false, "Betik yolu boş");

        var path = script.Path.Trim();
        if (!File.Exists(path))
            return (false, "Dosya bulunamadı");

        var extension = System.IO.Path.GetExtension(path).ToLowerInvariant();
        if (!Supported.Contains(extension))
            return (false, "Desteklenmeyen tür (ps1, bat, cmd, py, exe)");

        try
        {
            var info = new ProcessStartInfo
            {
                UseShellExecute = false,
                CreateNoWindow = true,
                WorkingDirectory = System.IO.Path.GetDirectoryName(path) ?? Environment.CurrentDirectory
            };

            if (extension == ".ps1")
            {
                info.FileName = "powershell.exe";
                info.ArgumentList.Add("-NoProfile");
                info.ArgumentList.Add("-ExecutionPolicy");
                info.ArgumentList.Add("Bypass");
                info.ArgumentList.Add("-WindowStyle");
                info.ArgumentList.Add("Hidden");
                info.ArgumentList.Add("-File");
                info.ArgumentList.Add(path);
            }
            else if (extension is ".bat" or ".cmd")
            {
                info.FileName = "cmd.exe";
                info.ArgumentList.Add("/c");
                info.ArgumentList.Add(path);
            }
            else if (extension == ".py")
            {
                info.FileName = "python.exe";
                info.ArgumentList.Add("-u");
                info.ArgumentList.Add(path);
            }
            else
            {
                info.FileName = path;
            }

            if (!string.IsNullOrWhiteSpace(script.Arguments))
            {
                foreach (var argument in SplitArguments(script.Arguments))
                    info.ArgumentList.Add(argument);
            }

            using var process = Process.Start(info);
            if (process == null)
                return (false, "Süreç başlatılamadı");

            return (true, $"Başlatıldı (PID {process.Id})");
        }
        catch (Exception ex)
        {
            AppLog.Error("ScriptRunner", ex);
            return (false, ex is System.ComponentModel.Win32Exception
                ? "Başlatılamadı; Python veya PowerShell kurulu olmayabilir"
                : ex.Message);
        }
    }

    private static IEnumerable<string> SplitArguments(string text)
    {
        var current = new System.Text.StringBuilder();
        var quoted = false;

        foreach (var ch in text)
        {
            if (ch == '"')
            {
                quoted = !quoted;
                continue;
            }

            if (char.IsWhiteSpace(ch) && !quoted)
            {
                if (current.Length > 0)
                {
                    yield return current.ToString();
                    current.Clear();
                }

                continue;
            }

            current.Append(ch);
        }

        if (current.Length > 0)
            yield return current.ToString();
    }
}
