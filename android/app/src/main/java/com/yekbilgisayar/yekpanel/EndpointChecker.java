package com.yekbilgisayar.yekpanel;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/** Sunucu uçlarını HTTP durum kodu, gecikme ve SSL bitiş süresiyle kontrol eder. */
public final class EndpointChecker {

    public static final class Result {
        public int code = -1;
        public long latencyMs = -1;
        public int sslDays = -1;
        public String error = "";

        public boolean reachable() {
            return code > 0;
        }

        public boolean healthy() {
            return code >= 200 && code < 400;
        }
    }

    private EndpointChecker() {
    }

    public static Result check(String url) {
        Result result = new Result();
        HttpURLConnection connection = null;
        long start = System.nanoTime();
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Range", "bytes=0-0");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("User-Agent", "YEKPanel-Android/1.1");
            result.code = connection.getResponseCode();
            result.latencyMs = (System.nanoTime() - start) / 1_000_000L;

            if (connection instanceof HttpsURLConnection) {
                result.sslDays = sslDays((HttpsURLConnection) connection);
            }
        } catch (Exception error) {
            result.latencyMs = (System.nanoTime() - start) / 1_000_000L;
            result.error = shortError(error);
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private static int sslDays(HttpsURLConnection connection) {
        try {
            int best = Integer.MAX_VALUE;
            for (Certificate certificate : connection.getServerCertificates()) {
                if (certificate instanceof X509Certificate) {
                    long notAfter = ((X509Certificate) certificate).getNotAfter().getTime();
                    int days = (int) ((notAfter - System.currentTimeMillis()) / 86_400_000L);
                    best = Math.min(best, days);
                }
            }
            return best == Integer.MAX_VALUE ? -1 : best;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static String shortError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        message = message.trim();
        return message.length() > 70 ? message.substring(0, 70) + "…" : message;
    }

    public static String codeLabel(Result result) {
        if (result == null) {
            return "—";
        }
        if (result.code > 0) {
            return String.valueOf(result.code);
        }
        return "KAPALI";
    }

    public static String latencyLabel(Result result) {
        if (result == null || result.latencyMs < 0) {
            return "—";
        }
        return String.format(Locale.getDefault(), "%d ms", result.latencyMs);
    }
}
