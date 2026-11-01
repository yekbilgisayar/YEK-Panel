using System.Net.Security;
using System.Net.Sockets;
using System.Security.Cryptography.X509Certificates;

namespace YekPanel.Services;

public sealed record CertificateInfo(int DaysLeft, DateTime NotAfter, string Issuer, bool SelfSigned);

public static class CertificateService
{
    private static readonly TimeSpan SuccessTtl = TimeSpan.FromHours(6);
    private static readonly TimeSpan FailureTtl = TimeSpan.FromMinutes(10);
    private static readonly Dictionary<string, (DateTime FetchedUtc, CertificateInfo? Info)> Cache = new();

    public static async Task<CertificateInfo?> GetAsync(string url, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(url))
            return null;

        var normalized = url.Trim();
        if (normalized.IndexOf("://", StringComparison.Ordinal) < 0)
            normalized = "http://" + normalized;

        if (!Uri.TryCreate(normalized, UriKind.Absolute, out var uri) ||
            uri.Scheme != Uri.UriSchemeHttps)
            return null;

        var port = uri.IsDefaultPort ? 443 : uri.Port;
        var key = uri.Host + ":" + port;

        lock (Cache)
        {
            if (Cache.TryGetValue(key, out var cached))
            {
                var ttl = cached.Info == null ? FailureTtl : SuccessTtl;
                if (DateTime.UtcNow - cached.FetchedUtc < ttl)
                    return cached.Info;
            }
        }

        var info = await ProbeAsync(uri.Host, port, ct);

        lock (Cache)
        {
            Cache[key] = (DateTime.UtcNow, info);
        }

        return info;
    }

    private static async Task<CertificateInfo?> ProbeAsync(string host, int port, CancellationToken ct)
    {
        try
        {
            using var tcp = new TcpClient();
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
            timeout.CancelAfter(TimeSpan.FromSeconds(5));

            await tcp.ConnectAsync(host, port, timeout.Token);

            using var ssl = new SslStream(tcp.GetStream(), leaveInnerStreamOpen: false, (_, _, _, _) => true);
            await ssl.AuthenticateAsClientAsync(
                new SslClientAuthenticationOptions { TargetHost = host },
                timeout.Token);

            if (ssl.RemoteCertificate is not { } remote)
                return null;

            using var certificate = X509CertificateLoader.LoadCertificate(remote.GetRawCertData());
            var daysLeft = (certificate.NotAfter.Date - DateTime.Today).Days;
            return new CertificateInfo(
                daysLeft,
                certificate.NotAfter,
                ShortIssuer(certificate.Issuer),
                string.Equals(certificate.Subject, certificate.Issuer, StringComparison.OrdinalIgnoreCase));
        }
        catch
        {
            return null;
        }
    }

    private static string ShortIssuer(string issuer)
    {
        var value = issuer;
        var index = value.IndexOf("CN=", StringComparison.OrdinalIgnoreCase);
        if (index >= 0)
        {
            value = value[(index + 3)..];
            var comma = value.IndexOf(',');
            if (comma >= 0)
                value = value[..comma];
        }

        value = value.Trim().Trim('"');
        return value.Length > 40 ? value[..40] : value;
    }
}
