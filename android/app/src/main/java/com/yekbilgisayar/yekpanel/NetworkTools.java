package com.yekbilgisayar.yekpanel;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Dış IP, DNS çözümleme ve TCP port testi; tümü kullanıcı isteğiyle çalışır. */
public final class NetworkTools {

    public static final class ExternalInfo {
        public String ip = "";
        public String city = "";
        public String country = "";
        public String error = "";

        public boolean ok() {
            return error.isEmpty() && !ip.isEmpty();
        }

        public String summary() {
            if (!ok()) {
                return error;
            }
            String place = city;
            if (country != null && !country.isEmpty()) {
                place = place.isEmpty() ? country : place + ", " + country;
            }
            return place.isEmpty() ? ip : ip + " · " + place;
        }
    }

    public static final class DnsResult {
        public final List<String> addresses = new ArrayList<>();
        public long elapsedMs = -1;
        public String error = "";
    }

    public static final class PortResult {
        public boolean open = false;
        public long elapsedMs = -1;
        public String error = "";
    }

    private NetworkTools() {
    }

    public static ExternalInfo externalIp() {
        ExternalInfo info = new ExternalInfo();
        if (fromIpWhoIs(info)) {
            return info;
        }
        fromIpify(info);
        return info;
    }

    private static boolean fromIpWhoIs(ExternalInfo info) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://ipwho.is/").openConnection();
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("User-Agent", "YEKPanel-Android/1.1");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null && body.length() < 8192) {
                    body.append(line);
                }
                JSONObject object = new JSONObject(body.toString());
                if (!object.optBoolean("success", true)) {
                    return false;
                }
                info.ip = object.optString("ip");
                info.city = object.optString("city");
                info.country = object.optString("country");
                if (!info.ip.isEmpty()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    private static boolean fromIpify(ExternalInfo info) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://api.ipify.org?format=json")
                    .openConnection();
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("User-Agent", "YEKPanel-Android/1.1");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                JSONObject object = new JSONObject(reader.readLine());
                info.ip = object.optString("ip");
                info.city = "";
                info.country = "";
                if (!info.ip.isEmpty()) {
                    info.error = "";
                    return true;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
        info.error = "Dış IP alınamadı";
        return false;
    }

    public static DnsResult resolve(String rawHost) {
        DnsResult result = new DnsResult();
        String host = cleanHost(rawHost);
        if (host.isEmpty()) {
            result.error = "Geçerli bir adres girin";
            return result;
        }
        long start = System.nanoTime();
        try {
            Set<String> unique = new LinkedHashSet<>();
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.getHostAddress() != null) {
                    unique.add(address.getHostAddress());
                }
            }
            result.addresses.addAll(unique);
            result.elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            if (result.addresses.isEmpty()) {
                result.error = "Kayıt bulunamadı";
            }
        } catch (Exception error) {
            result.elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            result.error = "Çözümlenemedi";
        }
        return result;
    }

    public static PortResult port(String rawHost, int port) {
        PortResult result = new PortResult();
        String host = cleanHost(rawHost);
        if (host.isEmpty() || port < 1 || port > 65535) {
            result.error = "Adres veya port geçersiz";
            return result;
        }
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 4000);
            result.open = true;
        } catch (Exception ignored) {
            result.open = false;
        }
        result.elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        return result;
    }

    public static String cleanHost(String raw) {
        if (raw == null) {
            return "";
        }
        String host = raw.trim();
        if (host.toLowerCase(Locale.ROOT).startsWith("https://")) {
            host = host.substring(8);
        } else if (host.toLowerCase(Locale.ROOT).startsWith("http://")) {
            host = host.substring(7);
        }
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(':') == colon) {
            host = host.substring(0, colon);
        }
        return host.trim();
    }
}
