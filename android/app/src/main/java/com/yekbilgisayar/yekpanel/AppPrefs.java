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

    public static final int DEFAULT_LOW = 20;
    public static final int DEFAULT_HIGH = 90;
    public static final int DEFAULT_TEMP = 45;
    public static final int DEFAULT_QUIET_START = 23 * 60;
    public static final int DEFAULT_QUIET_END = 8 * 60;
    public static final int DEFAULT_REFRESH = 2;

    private AppPrefs() {
    }
}
