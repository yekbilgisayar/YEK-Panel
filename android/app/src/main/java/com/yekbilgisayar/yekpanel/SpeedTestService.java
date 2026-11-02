package com.yekbilgisayar.yekpanel;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Kısa indirme hızı testi. Tek bir sağlayıcıya bağlı kalmamak için sırayla
 * OVH, Cloudflare ve Hetzner denenir; 8 MB veya 12 saniyede test kesilir.
 */
public final class SpeedTestService {

    public static final class Result {
        public double mbps;
        public double megabytes;
        public long elapsedMs;
        public String server = "";
        public String error = "";

        public boolean ok() {
            return error.isEmpty() && mbps > 0;
        }
    }

    private static final String[] URLS = {
            "https://proof.ovh.net/files/10Mb.dat",
            "https://speed.cloudflare.com/__down?bytes=10000000",
            "https://speed.hetzner.de/10MB.bin"
    };

    private SpeedTestService() {
    }

    public static Result run() {
        Result last = null;
        for (String url : URLS) {
            Result result = measure(url);
            if (result.ok()) {
                return result;
            }
            last = result;
        }
        if (last == null) {
            last = new Result();
            last.error = "Test sunucusuna ulaşılamadı";
        }
        return last;
    }

    private static Result measure(String url) {
        Result result = new Result();
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("User-Agent", "YEKPanel-Android/1.1");
            connection.setRequestProperty("Range", "bytes=0-7999999");
            connection.setInstanceFollowRedirects(true);

            long start = System.nanoTime();
            long total = 0;
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[32768];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    total += read;
                    if (total >= 8_000_000L
                            || (System.nanoTime() - start) / 1_000_000L > 12_000L) {
                        break;
                    }
                }
            }
            long elapsed = Math.max(1, (System.nanoTime() - start) / 1_000_000L);
            if (total < 200_000L) {
                result.error = "Yetersiz veri alındı";
                return result;
            }
            result.elapsedMs = elapsed;
            result.megabytes = total / 1_048_576.0;
            result.mbps = total * 8.0 / (elapsed / 1000.0) / 1_000_000.0;
            result.server = new URL(url).getHost();
            return result;
        } catch (Exception error) {
            result.error = "Test başarısız";
            return result;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static String summary(Result result) {
        if (result == null) {
            return "—";
        }
        if (!result.ok()) {
            return result.error;
        }
        return String.format(Locale.getDefault(),
                "%.1f Mbps%n%.1f MB · %.1f sn · %s",
                result.mbps, result.megabytes, result.elapsedMs / 1000.0, result.server);
    }
}
