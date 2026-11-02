package com.yekbilgisayar.yekpanel;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.Display;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

/**
 * Cihazın anlık sistem verilerini okur. Tüm okumalar savunmacıdır:
 * üretici kısıtlamalarında ilgili alan "bilinmiyor" olarak döner.
 */
public final class DeviceStats {

    public static final class Snapshot {
        public String manufacturer = "";
        public String model = "";
        public String android = "";
        public int sdk = 0;
        public long uptimeMs = 0;

        public int batteryPercent = -1;
        public boolean charging = false;
        public double batteryTempC = -1;
        public int batteryVoltageMv = -1;
        public String batteryHealth = "—";
        public String batteryTechnology = "—";
        public long batteryCurrentUa = Long.MIN_VALUE;
        public long batteryChargeCounterUah = -1;
        public int batteryCapacity = -1;
        public String powerSource = "Pil";

        public long ramTotal = 0;
        public long ramAvail = 0;
        public int ramPercent = -1;

        public long storageTotal = 0;
        public long storageFree = 0;
        public int storagePercent = -1;

        public int cores = 0;
        public double cpuFreqGhz = 0;
        public int cpuLoad = -1;
        public double load1 = -1;
        public String soc = "";

        public String securityPatch = "—";
        public String kernel = "—";
        public String abi = "—";
        public int screenWidth = 0;
        public int screenHeight = 0;
        public int screenDpi = 0;
        public float refreshRate = 0;

        public String connType = "Bağlantı yok";
        public String localIp = "—";
        public String gateway = "—";
        public String dns = "—";
        public int signalDbm = Integer.MIN_VALUE;
        public int linkDownKbps = -1;
        public int linkUpKbps = -1;
        public boolean connected = false;
        public boolean wifi = false;
    }

    private static long lastTotal = -1;
    private static long lastIdle = -1;

    private DeviceStats() {
    }

    public static Snapshot read(Context context) {
        Snapshot s = new Snapshot();
        s.manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        s.model = Build.MODEL == null ? "Android" : Build.MODEL;
        s.android = "Android " + Build.VERSION.RELEASE;
        s.sdk = Build.VERSION.SDK_INT;
        s.uptimeMs = SystemClock.elapsedRealtime();

        readBattery(context, s);
        readMemory(context, s);
        readStorage(s);
        readCpu(s);
        readBuildDetails(s);
        readDisplay(context, s);
        readNetwork(context, s);
        return s;
    }

    private static void readBattery(Context context, Snapshot s) {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent battery = context.registerReceiver(null, filter);
            if (battery == null) {
                return;
            }
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                s.batteryPercent = Math.round(level * 100f / scale);
            }
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            s.charging = plugged
                    || status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;

            int temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            if (temp != Integer.MIN_VALUE) {
                s.batteryTempC = temp / 10.0;
            }
            int voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            if (voltage > 0) {
                s.batteryVoltageMv = voltage;
            }
            int health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            s.batteryHealth = healthLabel(health);

            String technology = battery.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
            if (technology != null && !technology.trim().isEmpty()) {
                s.batteryTechnology = technology.trim();
            }
            int pluggedSource = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            s.powerSource = powerSourceLabel(pluggedSource);

            BatteryManager manager =
                    (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (manager != null) {
                long current = manager.getLongProperty(
                        BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                if (current != Long.MIN_VALUE && current != Long.MAX_VALUE) {
                    s.batteryCurrentUa = current;
                }
                long charge = manager.getLongProperty(
                        BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                if (charge > 0) {
                    s.batteryChargeCounterUah = charge;
                }
                int capacity = manager.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (capacity >= 0) {
                    s.batteryCapacity = capacity;
                }
            }
        } catch (Exception ignored) {
            // Üretici kısıtlaması: pil bilgisi okunamadı.
        }
    }

    private static String powerSourceLabel(int plugged) {
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "AC adaptör";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "Kablosuz";
            case 8:
                return "Dock";
            default:
                return "Pil";
        }
    }

    private static String healthLabel(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "İyi";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Aşırı sıcak";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Ömrü bitmiş";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Aşırı voltaj";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "Arızalı";
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Soğuk";
            default:
                return "—";
        }
    }

    private static void readMemory(Context context, Snapshot s) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return;
            }
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            s.ramTotal = info.totalMem;
            s.ramAvail = info.availMem;
            if (info.totalMem > 0) {
                long used = info.totalMem - info.availMem;
                s.ramPercent = (int) Math.min(100, Math.round(used * 100.0 / info.totalMem));
            }
        } catch (Exception ignored) {
        }
    }

    private static void readStorage(Snapshot s) {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            s.storageTotal = stat.getTotalBytes();
            s.storageFree = stat.getAvailableBytes();
            if (s.storageTotal > 0) {
                long used = s.storageTotal - s.storageFree;
                s.storagePercent = (int) Math.min(100, Math.round(used * 100.0 / s.storageTotal));
            }
        } catch (Exception ignored) {
        }
    }

    private static void readCpu(Snapshot s) {
        s.cores = Runtime.getRuntime().availableProcessors();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL != null
                && !Build.SOC_MODEL.equalsIgnoreCase("unknown")) {
            s.soc = Build.SOC_MODEL;
        } else if (Build.HARDWARE != null) {
            s.soc = Build.HARDWARE;
        }
        s.cpuFreqGhz = readAverageFrequencyGhz();
        s.cpuLoad = readCpuLoad();
        s.load1 = readLoadAverage();
    }

    private static double readAverageFrequencyGhz() {
        try {
            File base = new File("/sys/devices/system/cpu");
            File[] dirs = base.listFiles((dir, name) -> name.matches("cpu[0-9]+"));
            if (dirs == null || dirs.length == 0) {
                return 0;
            }
            double sum = 0;
            int count = 0;
            for (File dir : dirs) {
                long khz = readLong(new File(dir, "cpufreq/scaling_cur_freq"));
                if (khz <= 0) {
                    khz = readLong(new File(dir, "cpufreq/cpuinfo_cur_freq"));
                }
                if (khz > 0) {
                    sum += khz;
                    count++;
                }
            }
            if (count == 0) {
                return 0;
            }
            return sum / count / 1_000_000.0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static long readLong(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line == null) {
                return -1;
            }
            return Long.parseLong(line.trim());
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static int readCpuLoad() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("cpu ")) {
                return -1;
            }
            String[] parts = line.trim().split("\\s+");
            long total = 0;
            for (int i = 1; i < parts.length; i++) {
                total += Long.parseLong(parts[i]);
            }
            long idle = Long.parseLong(parts[4]);
            if (parts.length > 5) {
                idle += Long.parseLong(parts[5]);
            }
            int result;
            if (lastTotal < 0 || total <= lastTotal) {
                result = lastTotal < 0 ? 0 : -1;
            } else {
                long dTotal = total - lastTotal;
                long dIdle = idle - lastIdle;
                result = (int) Math.max(0, Math.min(100, Math.round((dTotal - dIdle) * 100.0 / dTotal)));
            }
            lastTotal = total;
            lastIdle = idle;
            return result;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static double readLoadAverage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/loadavg"))) {
            String line = reader.readLine();
            if (line == null) {
                return -1;
            }
            return Double.parseDouble(line.split("\\s+")[0]);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static void readBuildDetails(Snapshot s) {
        try {
            if (Build.VERSION.SECURITY_PATCH != null
                    && !Build.VERSION.SECURITY_PATCH.trim().isEmpty()) {
                s.securityPatch = Build.VERSION.SECURITY_PATCH;
            }
            String kernel = System.getProperty("os.version");
            if (kernel != null && !kernel.trim().isEmpty()) {
                s.kernel = kernel;
            }
            if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
                s.abi = Build.SUPPORTED_ABIS[0];
            }
        } catch (Exception ignored) {
        }
    }

    private static void readDisplay(Context context, Snapshot s) {
        try {
            DisplayManager manager =
                    (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            if (manager == null) {
                return;
            }
            Display display = manager.getDisplay(Display.DEFAULT_DISPLAY);
            if (display == null) {
                return;
            }
            Display.Mode mode = display.getMode();
            if (mode != null) {
                s.screenWidth = mode.getPhysicalWidth();
                s.screenHeight = mode.getPhysicalHeight();
                s.refreshRate = mode.getRefreshRate();
            }
            s.screenDpi = context.getResources().getDisplayMetrics().densityDpi;
        } catch (Exception ignored) {
        }
    }

    private static void readNetwork(Context context, Snapshot s) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return;
            }
            Network active = cm.getActiveNetwork();
            if (active == null) {
                return;
            }
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            if (caps == null) {
                return;
            }
            s.connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            int downstream = caps.getLinkDownstreamBandwidthKbps();
            int upstream = caps.getLinkUpstreamBandwidthKbps();
            if (downstream > 0) {
                s.linkDownKbps = downstream;
            }
            if (upstream > 0) {
                s.linkUpKbps = upstream;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int strength = caps.getSignalStrength();
                if (strength > -120 && strength < 0) {
                    s.signalDbm = strength;
                }
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                s.connType = "Wi-Fi";
                s.wifi = true;
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                s.connType = "Mobil veri";
                try {
                    TelephonyManager tm =
                            (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (tm != null) {
                        String operator = tm.getNetworkOperatorName();
                        if (operator != null && !operator.isEmpty()) {
                            s.connType += " · " + operator;
                        }
                    }
                } catch (Exception ignored) {
                }
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                s.connType = "Ethernet";
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                s.connType = "VPN";
            } else {
                s.connType = "Bağlı";
            }
            LinkProperties props = cm.getLinkProperties(active);
            if (props != null) {
                s.localIp = firstIpv4(props.getLinkAddresses());
                s.gateway = defaultGateway(props);
                s.dns = dnsServers(props);
            }
        } catch (Exception ignored) {
        }
    }

    private static String defaultGateway(LinkProperties props) {
        try {
            for (RouteInfo route : props.getRoutes()) {
                InetAddress gateway = route.getGateway();
                if (route.isDefaultRoute() && gateway != null
                        && !gateway.isAnyLocalAddress() && !gateway.isLinkLocalAddress()) {
                    return gateway.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        return "—";
    }

    private static String dnsServers(LinkProperties props) {
        try {
            List<InetAddress> servers = props.getDnsServers();
            if (servers == null || servers.isEmpty()) {
                return "—";
            }
            StringBuilder builder = new StringBuilder();
            int count = 0;
            for (InetAddress server : servers) {
                if (server.getHostAddress() == null) {
                    continue;
                }
                if (count > 0) {
                    builder.append(" · ");
                }
                builder.append(server.getHostAddress());
                count++;
                if (count >= 2) {
                    break;
                }
            }
            return count == 0 ? "—" : builder.toString();
        } catch (Exception ignored) {
            return "—";
        }
    }

    private static String firstIpv4(List<LinkAddress> addresses) {
        if (addresses == null) {
            return "—";
        }
        for (LinkAddress addr : addresses) {
            if (addr.getAddress() != null && addr.getAddress().getAddress() != null
                    && addr.getAddress().getAddress().length == 4) {
                return addr.getAddress().getHostAddress();
            }
        }
        return "—";
    }

    // ---- Biçimlendirme yardımcıları ----

    public static String gb(long bytes) {
        if (bytes <= 0) {
            return "—";
        }
        double value = bytes / 1073741824.0;
        if (value >= 100) {
            return String.format(Locale.getDefault(), "%.0f GB", value);
        }
        return String.format(Locale.getDefault(), "%.1f GB", value);
    }

    public static String uptime(long ms) {
        long totalMinutes = ms / 60000;
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes / 60) % 24;
        long minutes = totalMinutes % 60;
        if (days > 0) {
            return days + " gün " + hours + " sa";
        }
        if (hours > 0) {
            return hours + " sa " + minutes + " dk";
        }
        return minutes + " dk";
    }

    public static String speed(int kbps) {
        if (kbps <= 0) {
            return "—";
        }
        if (kbps >= 1000) {
            return String.format(Locale.getDefault(), "%.1f Mbps", kbps / 1000.0);
        }
        return kbps + " kbps";
    }

    public static String signal(int dbm) {
        if (dbm <= -120 || dbm >= 0) {
            return "—";
        }
        return dbm + " dBm";
    }
}
