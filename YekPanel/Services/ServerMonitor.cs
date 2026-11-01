using System.Diagnostics;
using System.Net.Http;
using System.Net.Sockets;

namespace YekPanel.Services;

public sealed class ServerCheckResult
{
    public bool Responded { get; init; }
    public int? StatusCode { get; init; }
    public long LatencyMs { get; init; }
    public string? Error { get; init; }
    public string? Note { get; init; }
    public bool PortChecked { get; init; }
    public bool PortOpen { get; init; }
    public int Port { get; init; }
}

public static class ServerMonitor
{
    public static async Task<ServerCheckResult> CheckAsync(HttpClient http, string url, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(url))
            return new ServerCheckResult { Error = "Adres girilmedi" };

        var normalized = url.Trim();
        if (normalized.IndexOf("://", StringComparison.Ordinal) < 0)
            normalized = "http://" + normalized;

        if (!Uri.TryCreate(normalized, UriKind.Absolute, out var uri) ||
            (uri.Scheme != Uri.UriSchemeHttp && uri.Scheme != Uri.UriSchemeHttps))
            return new ServerCheckResult { Error = "Adres geçersiz" };

        var port = uri.IsDefaultPort
            ? (uri.Scheme == Uri.UriSchemeHttps ? 443 : 80)
            : uri.Port;

        // Önce gerçek TCP bağlantısı denenir; kapalıysa HTTP beklemeye gerek yok.
        var (tcpOpen, tcpMs) = await TcpProbeAsync(uri.Host, port, ct);
        if (!tcpOpen)
        {
            return new ServerCheckResult
            {
                LatencyMs = tcpMs,
                Error = $"TCP {port} kapalı",
                PortChecked = true,
                PortOpen = false,
                Port = port
            };
        }

        var result = await AttemptAsync(http, uri, ct);

        // Yerel servislerde HTTPS yazılmış ama sunucu HTTP konuşuyor olabilir.
        if (!result.Responded && uri.Scheme == Uri.UriSchemeHttps && result.Error == "Bağlantı kurulamadı")
        {
            var fallback = new UriBuilder(uri)
            {
                Scheme = Uri.UriSchemeHttp,
                Port = uri.IsDefaultPort ? 80 : uri.Port
            };

            var retry = await AttemptAsync(http, fallback.Uri, ct);
            if (retry.Responded)
                return new ServerCheckResult
                {
                    Responded = true,
                    StatusCode = retry.StatusCode,
                    LatencyMs = retry.LatencyMs,
                    Note = "HTTP ile bağlandı (HTTPS yanıt vermedi)",
                    PortChecked = true,
                    PortOpen = true,
                    Port = port
                };
        }

        return new ServerCheckResult
        {
            Responded = result.Responded,
            StatusCode = result.StatusCode,
            LatencyMs = result.LatencyMs,
            Error = result.Error,
            Note = result.Note,
            PortChecked = true,
            PortOpen = true,
            Port = port
        };
    }

    private static async Task<(bool Open, long ElapsedMs)> TcpProbeAsync(
        string host, int port, CancellationToken ct)
    {
        var stopwatch = Stopwatch.StartNew();
        try
        {
            using var client = new TcpClient();
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
            timeout.CancelAfter(TimeSpan.FromSeconds(4));
            await client.ConnectAsync(host, port, timeout.Token);
            stopwatch.Stop();
            return (true, stopwatch.ElapsedMilliseconds);
        }
        catch
        {
            stopwatch.Stop();
            return (false, stopwatch.ElapsedMilliseconds);
        }
    }

    private static async Task<ServerCheckResult> AttemptAsync(HttpClient http, Uri uri, CancellationToken ct)
    {
        var stopwatch = Stopwatch.StartNew();
        try
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, uri);
            request.Headers.UserAgent.ParseAdd("YekPanel/1.0");
            using var response = await http.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, ct);
            stopwatch.Stop();

            return new ServerCheckResult
            {
                Responded = true,
                StatusCode = (int)response.StatusCode,
                LatencyMs = stopwatch.ElapsedMilliseconds
            };
        }
        catch (OperationCanceledException)
        {
            stopwatch.Stop();
            return new ServerCheckResult { LatencyMs = stopwatch.ElapsedMilliseconds, Error = "Zaman aşımı" };
        }
        catch (HttpRequestException)
        {
            stopwatch.Stop();
            return new ServerCheckResult { LatencyMs = stopwatch.ElapsedMilliseconds, Error = "Bağlantı kurulamadı" };
        }
        catch (Exception)
        {
            stopwatch.Stop();
            return new ServerCheckResult { LatencyMs = stopwatch.ElapsedMilliseconds, Error = "Bilinmeyen hata" };
        }
    }
}
