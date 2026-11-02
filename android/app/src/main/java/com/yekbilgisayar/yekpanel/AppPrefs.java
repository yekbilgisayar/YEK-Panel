package com.yekbilgisayar.yekpanel;

/** Uygulama ayarlarının tek kaynağı; yedekleme de aynı anahtarları kullanır. */
public final class AppPrefs {

    public static final String PREFS = "yek_settings";

    public static final String K_ALERTS = "alerts_enabled";
    public static final String K_LOW = "alert_low";
    public static final String K_HIGH = "alert_high";
    public static final String K_TEMP = "alert_temp";

    public static final String K_QUIET = "quiet_enabled";
    public static final String K_QUIET_START = "quiet_start";
    public static final String K_QUIET_END = "quiet_end";
    public static final String K_REFRESH = "refresh_seconds";

    public static final String K_PC_HOST = "pc_host";
    public static final String K_PC_PORT = "pc_port";
    public static final String K_PC_PIN = "pc_pin";
    public static final String K_PC_AUTO = "pc_auto_connect";

    public static final String K_UPDATE_AUTO = "update_auto_check";
    public static final String K_UPDATE_LAST = "update_last_check";

    public static final int DEFAULT_LOW = 20;
    public static final int DEFAULT_HIGH = 90;
    public static final int DEFAULT_TEMP = 45;
    public static final int DEFAULT_QUIET_START = 23 * 60;
    public static final int DEFAULT_QUIET_END = 8 * 60;
    public static final int DEFAULT_REFRESH = 2;
    public static final int DEFAULT_PC_PORT = 8787;
    public static final long UPDATE_INTERVAL = 24L * 60L * 60L * 1000L;

    private AppPrefs() {
    }
}
