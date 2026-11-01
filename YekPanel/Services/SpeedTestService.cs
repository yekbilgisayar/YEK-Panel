using System.Diagnostics;
using System.Net.Http;

namespace YekPanel.Services;

public static class SpeedTestService
{
    private const int MaxSeconds = 8;

    private static readonly string[] DownloadUrls =
    {
        "https://speed.cloudflare.com/__down?bytes=100000000",
        "https://proof.ovh.net/files/100Mb.dat",
        "http://ipv4.download.thinkbroadband.com/100MB.zip",
        "https://ash-speed.hetzner.com/100MB.bin"
    };

    private static readonly HttpClient Client = new(new SocketsHttpHandler
    {
        AutomaticDecompression = System.Net.DecompressionMethods.None,
        ConnectTimeout = TimeSpan.FromSeconds(4)
    })
    {
        Timeout = TimeSpan.FromSeconds(30)
    };

    /// <summary>İndirme hızını Mbps olarak ölçer. Ulaşılabilen ilk sunucu kullanılır.</summary>
    public static async Task<double?> MeasureDownloadAsync(CancellationToken ct)
    {
        foreach (var url in DownloadUrls)
        {
            var result = await TryMeasureAsync(url, ct);
            if (result != null)
                return result;
        }

        return null;
    }

    private static async Task<double?> TryMeasureAsync(string url, CancellationToken ct)
    {
        try
        {
            using var response = await Client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, ct);
            if (!response.IsSuccessStatusCode)
                return null;

            await using var stream = await response.Content.ReadAsStreamAsync(ct);
            var buffer = new byte[81920];
            var stopwatch = new Stopwatch();
            long total = 0;

            while (stopwatch.Elapsed < TimeSpan.FromSeconds(MaxSeconds))
            {
                if (total == 0)
                    stopwatch.Start();

                var read = await stream.ReadAsync(buffer, ct);
                if (read == 0)
                    break;

                total += read;
            }

            stopwatch.Stop();
            if (total < 500_000 || stopwatch.Elapsed.TotalSeconds < 0.2)
                return null;

            return total * 8 / stopwatch.Elapsed.TotalSeconds / 1_000_000;
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
            throw;
        }
        catch
        {
            return null;
        }
    }
}
