using System.Globalization;
using System.IO;
using System.Text;
using YekPanel.Models;

namespace YekPanel.Services;

public static class ReportExporter
{
    private static readonly CultureInfo Tr = new("tr-TR");

    private static readonly string Dir =
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "YekPanel");

    public static string? Export(AppSettings settings, double cpuPercent, double ramPercent, double diskPercent,
        int days = 7)
    {
        try
        {
            days = Math.Clamp(days, 1, 31);
            var now = DateTime.Now;
            var path = Path.Combine(Dir, $"rapor-{now:yyyyMMdd-HHmmss}.csv");
            var builder = new StringBuilder();
            builder.Append('\uFEFF');
            builder.AppendLine($"YEK Panel raporu;{now.ToString("dd.MM.yyyy HH:mm", Tr)};{days} gun");
            builder.AppendLine($"SISTEM;CPU %{cpuPercent.ToString("0", Tr)};RAM %{ramPercent.ToString("0", Tr)};DISK %{diskPercent.ToString("0", Tr)}");
            builder.AppendLine();

            builder.AppendLine("SUNUCU;TARIH;CEVRIMICI %");
            foreach (var endpoint in settings.Endpoints.Where(e => !string.IsNullOrWhiteSpace(e.Url)))
            {
                foreach (var day in UptimeHistory.Daily(endpoint.Name, days))
                {
                    var value = day.Percent is double percent
                        ? percent.ToString("0.0", Tr)
                        : "-";
                    builder.AppendLine($"{endpoint.Name};{day.Day.ToString("dd.MM.yyyy", Tr)};{value}");
                }
            }

            builder.AppendLine();
            builder.AppendLine("KESINTI;SUNUCU;BASLANGIC;SURE");
            foreach (var incident in UptimeHistory.Incidents(days, 100))
            {
                builder.AppendLine(
                    $"KESINTI;{incident.Server};{incident.StartLocal.ToString("dd.MM.yyyy HH:mm", Tr)};" +
                    (incident.Ongoing ? "devam ediyor" : FormatDuration(incident.Duration)));
            }

            builder.AppendLine();
            builder.AppendLine("SUNUCU;ZAMAN;DURUM");
            foreach (var entry in NotificationLog.Recent(100))
            {
                builder.AppendLine(
                    $"{entry.Server};{entry.Utc.ToLocalTime().ToString("dd.MM.yyyy HH:mm", Tr)};" +
                    (entry.Recovered ? "duzeldi" : "sorun"));
            }

            Directory.CreateDirectory(Dir);
            File.WriteAllText(path, builder.ToString(), new UTF8Encoding(encoderShouldEmitUTF8Identifier: true));
            return path;
        }
        catch
        {
            return null;
        }
    }

    private static string FormatDuration(TimeSpan duration) =>
        duration.TotalHours >= 1
            ? $"{(int)duration.TotalHours} sa {duration.Minutes} dk"
            : duration.TotalMinutes >= 1
                ? $"{(int)duration.TotalMinutes} dk"
                : $"{Math.Max(1, (int)duration.TotalSeconds)} sn";
}
