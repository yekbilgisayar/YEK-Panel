package com.yekbilgisayar.yekpanel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * Windows tarafındaki YEK Panel PC bağlantısı ile yerel ağ üzerinden konuşur.
 * İstekler yalnızca kullanıcının verdiği adrese gider; internete veri gönderilmez.
 */
public final class PcClient {

    public static final int DEFAULT_PORT = 8787;
    public static final int DISCOVERY_PORT = 8788;
    public static final String DISCOVERY_MESSAGE = "YEK-PANEL-DISCOVER";
    public static final String TOKEN_HEADER = "X-Yek-Token";

    public static final class Disk {
        public String name = "";
        public long total = 0;
        public long free = 0;
        public int percent = 0;
    }

    public static final class ProcessInfo {
        public int pid = 0;
        public String name = "";
        public double memMb = 0;
    }

    public static final class Status {
        public String app = "";
        public String version = "";
        public String name = "";
        public String os = "";
        public long uptimeSec = 0;
        public double cpuPercent = 0;
        public long ramTotal = 0;
        public long ramUsed = 0;
        public int ramPercent = 0;
        public long netDown = 0;
        public long netUp = 0;
        public long linkMbps = 0;
        public int batteryPercent = -1;
        public boolean batteryCharging = false;
        public double tempC = Double.NaN;
        public long serverTs = 0;
        public long receivedAt = 0;
        public final List<String> ips = new ArrayList<>();
        public final List<Disk> disks = new ArrayList<>();
        public final List<ProcessInfo> processes = new ArrayList<>();
    }

    public static final class Result {
        public Status status;
        public String error = "";
        public boolean unauthorized = false;
        public boolean reachable = false;
    }

    public static final class Found {
        public String host = "";
        public int port = DEFAULT_PORT;
        public String name = "";
    }

    private PcClient() {
    }

    public static String normalizeHost(String raw) {
        if (raw == null) {
            return "";
        }
        String host = raw.trim();
        if (host.startsWith("http://")) {
            host = host.substring(7);
        } else if (host.startsWith("https://")) {
            host = host.substring(8);
        }
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(']') < 0 && host.indexOf(':') == colon) {
            host = host.substring(0, colon);
        }
        return host.trim();
    }

    public static Result fetch(String host, int port, String pin) {
        Result result = new Result();
        HttpURLConnection connection = null;
        try {
            String cleanHost = normalizeHost(host);
            if (cleanHost.isEmpty()) {
                result.error = "Bilgisayar adresi boş";
                return result;
            }
            String urlHost = cleanHost.contains(":") && !cleanHost.startsWith("[")
                    ? "[" + cleanHost + "]" : cleanHost;
            URL url = new URL("http://" + urlHost + ":" + port + "/api/status");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty(TOKEN_HEADER, pin == null ? "" : pin.trim());
            connection.setRequestProperty("User-Agent",
                    "YEKPanel-Android/" + BuildConfig.VERSION_NAME);
            int code = connection.getResponseCode();
            if (code == 401) {
                result.unauthorized = true;
                result.error = "PIN hatalı";
                return result;
            }
            if (code == 429) {
                result.error = "Çok fazla hatalı PIN denemesi. 1 dakika bekleyin.";
                return result;
            }
            if (code != 200) {
                result.error = "PC yanıtı: HTTP " + code;
                return result;
            }
            String body = read(connection, 512 * 1024);
            JSONObject json = new JSONObject(body);
            if (!json.optBoolean("ok", false)) {
                result.error = "PC bağlantısı isteği reddetti";
                return result;
            }
            result.status = parseStatus(json);
            result.reachable = true;
        } catch (SocketTimeoutException error) {
            result.error = "PC yanıt vermedi (zaman aşımı)";
        } catch (ConnectException error) {
            result.error = "PC'ye ulaşılamadı. Köprü açık mı?";
        } catch (UnknownHostException error) {
            result.error = "Bilgisayar adresi bulunamadı";
        } catch (Exception error) {
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

    private static Status parseStatus(JSONObject json) {
        Status status = new Status();
        status.app = json.optString("app", "");
        status.version = json.optString("version", "");
        status.name = json.optString("name", "PC");
        status.os = json.optString("os", "");
        status.uptimeSec = json.optLong("uptimeSec", 0);
        status.cpuPercent = json.optDouble("cpuPercent", 0);
        status.ramTotal = json.optLong("ramTotal", 0);
        status.ramUsed = json.optLong("ramUsed", 0);
        status.ramPercent = json.optInt("ramPercent", 0);
        status.netDown = json.optLong("netDown", 0);
        status.netUp = json.optLong("netUp", 0);
        status.linkMbps = json.optLong("linkMbps", 0);
        status.batteryPercent = json.optInt("batteryPercent", -1);
        status.batteryCharging = json.optBoolean("batteryCharging", false);
        if (!json.isNull("tempC")) {
            status.tempC = json.optDouble("tempC", Double.NaN);
        }
        status.serverTs = json.optLong("ts", 0);
        status.receivedAt = System.currentTimeMillis();

        JSONArray ips = json.optJSONArray("ips");
        List<String> otherIps = new ArrayList<>();
        if (ips != null) {
            for (int i = 0; i < ips.length() && i < 12; i++) {
                String ip = ips.optString(i, "").trim();
                if (ip.isEmpty()) {
                    continue;
                }
                if (isLocalAddress(ip)) {
                    if (!status.ips.contains(ip)) {
                        status.ips.add(ip);
                    }
                } else if (!otherIps.contains(ip)) {
                    otherIps.add(ip);
                }
            }
        }
        // Yerel adres yoksa (ör. tümü CGNAT dışı) ilk birkaç adresi göster.
        if (status.ips.isEmpty()) {
            for (int i = 0; i < otherIps.size() && i < 3; i++) {
                status.ips.add(otherIps.get(i));
            }
        }
        JSONArray disks = json.optJSONArray("disks");
        if (disks != null) {
            for (int i = 0; i < disks.length() && i < 12; i++) {
                JSONObject item = disks.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                Disk disk = new Disk();
                disk.name = item.optString("name", "");
                disk.total = item.optLong("total", 0);
                disk.free = item.optLong("free", 0);
                disk.percent = item.optInt("percent", 0);
                status.disks.add(disk);
            }
        }
        JSONArray processes = json.optJSONArray("processes");
        if (processes != null) {
            for (int i = 0; i < processes.length() && i < 8; i++) {
                JSONObject item = processes.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                ProcessInfo process = new ProcessInfo();
                process.pid = item.optInt("pid", 0);
                process.name = item.optString("name", "");
                process.memMb = item.optDouble("memMb", 0);
                status.processes.add(process);
            }
        }
        return status;
    }

    /** 10/8, 172.16/12, 192.168/16, 169.254/16 ve 100.64/10 aralıklarını tanır. */
    static boolean isLocalAddress(String ip) {
        if (ip == null) {
            return false;
        }
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            if (first == 10 || first == 127) {
                return true;
            }
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            if (first == 192 && second == 168) {
                return true;
            }
            if (first == 169 && second == 254) {
                return true;
            }
            return first == 100 && second >= 64 && second <= 127;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private static String read(HttpURLConnection connection, int limit) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) > 0 && builder.length() < limit) {
                builder.append(buffer, 0, count);
            }
        }
        return builder.toString();
    }

    private static String shortError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Bağlantı kurulamadı";
        }
        String lower = message.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("failed to connect") || lower.contains("refused")) {
            return "PC'ye ulaşılamadı. Köprü açık mı?";
        }
        if (lower.contains("unreachable") || lower.contains("no route")) {
            return "Ağa ulaşılamıyor";
        }
        if (lower.contains("cleartext")) {
            return "Güvenli olmayan bağlantı engellendi";
        }
        if (lower.contains("permission")) {
            return "Ağ izni reddedildi";
        }
        return "Bağlantı kurulamadı";
    }

    /** Yerel ağa UDP keşif paketi gönderir; ilk yanıt veren köprüyü döndürür. */
    public static Found discover(int timeoutMs) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(350);
            byte[] message = DISCOVERY_MESSAGE.getBytes(StandardCharsets.UTF_8);
            for (InetAddress address : broadcastAddresses()) {
                try {
                    socket.send(new DatagramPacket(message, message.length,
                            address, DISCOVERY_PORT));
                } catch (Exception ignored) {
                }
            }
            long deadline = System.currentTimeMillis() + Math.max(800, timeoutMs);
            byte[] buffer = new byte[4096];
            while (System.currentTimeMillis() < deadline) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException ignored) {
                    continue;
                }
                try {
                    String text = new String(packet.getData(), 0, packet.getLength(),
                            StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(text);
                    if (!json.optBoolean("ok", false)) {
                        continue;
                    }
                    Found found = new Found();
                    found.host = packet.getAddress().getHostAddress();
                    found.port = json.optInt("port", DEFAULT_PORT);
                    found.name = json.optString("name", "PC");
                    if (found.port < 1 || found.port > 65535) {
                        found.port = DEFAULT_PORT;
                    }
                    return found;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static List<InetAddress> broadcastAddresses() {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            addresses.add(InetAddress.getByName("255.255.255.255"));
        } catch (Exception ignored) {
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback()) {
                    continue;
                }
                for (InterfaceAddress address : network.getInterfaceAddresses()) {
                    InetAddress broadcast = address.getBroadcast();
                    if (broadcast != null) {
                        addresses.add(broadcast);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return addresses;
    }

    public static String bytes(long value) {
        if (value <= 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double amount = value;
        int unit = 0;
        while (amount >= 1024 && unit < units.length - 1) {
            amount /= 1024;
            unit++;
        }
        return String.format(Locale.getDefault(), unit == 0 ? "%.0f %s" : "%.1f %s",
                amount, units[unit]);
    }

    public static String rate(long bytesPerSecond) {
        return bytes(bytesPerSecond) + "/sn";
    }

    public static String uptime(long seconds) {
        if (seconds <= 0) {
            return "—";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return days + " gün " + hours + " sa " + minutes + " dk";
        }
        if (hours > 0) {
            return hours + " sa " + minutes + " dk";
        }
        return Math.max(1, minutes) + " dk";
    }
}
