package com.yekbilgisayar.yekpanel;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.yekbilgisayar.yekpanel.ui.BatteryRingView;
import com.yekbilgisayar.yekpanel.ui.MeterView;
import com.yekbilgisayar.yekpanel.ui.SparkView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static final int REQ_NOTIFICATIONS = 31;
    private static final int REQ_EXPORT_TXT = 41;
    private static final int REQ_EXPORT_MD = 42;

    private static final String PREFS = "yek_settings";
    private static final String K_ALERTS = "alerts_enabled";
    private static final String K_LOW = "alert_low";
    private static final String K_HIGH = "alert_high";
    private static final String K_TEMP = "alert_temp";
    private static final long ALERT_COOLDOWN = 30L * 60L * 1000L;
    private static final String VERSION = "1.0.0";

    private int BG;
    private int ELEVATED;
    private int CARD;
    private int STROKE;
    private int TEXT;
    private int MUTED;
    private int ACCENT;
    private int GREEN;
    private int ORANGE;
    private int RED;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private NoteStore noteStore;
    private ReminderStore reminderStore;
    private DeviceStats.Snapshot stats;

    private FrameLayout pageHost;
    private ScrollView panelScroll;
    private ScrollView toolsScroll;
    private ScrollView settingsScroll;
    private int currentPage = 0;

    private final LinearLayout[] navItems = new LinearLayout[3];
    private final ImageView[] navIcons = new ImageView[3];
    private final TextView[] navLabels = new TextView[3];

    private BatteryRingView batteryRing;
    private TextView stateChip;
    private TextView batteryStatus;
    private TextView batteryMeta;
    private TextView batteryTempChip;
    private TextView batteryHealthChip;
    private TextView batteryHint;
    private TextView cpuValue;
    private TextView cpuMeta;
    private TextView cpuChip;
    private TextView cpuDetail;
    private SparkView cpuSpark;
    private TextView ramValue;
    private TextView ramSub;
    private MeterView ramMeter;
    private TextView storageValue;
    private TextView storageSub;
    private MeterView storageMeter;
    private TextView netType;
    private TextView netIp;
    private TextView netChip;
    private TextView sysDevice;
    private TextView sysAndroid;
    private TextView sysUptime;
    private TextView sysChip;

    private TextView toolsNetText;
    private TextView pingResult;
    private TextView pingButton;
    private ProgressBar pingProgress;
    private final TextView[] pingChips = new TextView[3];
    private int pingTargetIndex = 0;
    private boolean pinging = false;

    private EditText noteTitleInput;
    private EditText noteBodyInput;
    private TextView noteSaveButton;
    private TextView noteClearButton;
    private TextView noteCountChip;
    private LinearLayout noteList;
    private long editingNoteId = -1;
    private String pendingExport = "";

    private TextView reminderTimeButton;
    private EditText reminderLabelInput;
    private TextView reminderCountChip;
    private LinearLayout reminderList;
    private int reminderHour = 9;
    private int reminderMinute = 0;

    private Switch alertsSwitch;
    private SeekBar lowSeek;
    private SeekBar highSeek;
    private SeekBar tempSeek;
    private TextView notifButton;
    private TextView notifState;

    private int alertLow = 20;
    private int alertHigh = 90;
    private int alertTemp = 45;
    private boolean alertsEnabled = true;

    private final String[] pingTargets = {"1.1.1.1", "8.8.8.8", "9.9.9.9"};
    private final String[] pingLabels = {"CLOUDFLARE", "GOOGLE", "QUAD9"};

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (currentPage == 0) {
                refreshPanel();
            }
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPalette();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadAlertSettings();
        noteStore = new NoteStore(this);
        reminderStore = new ReminderStore(this);
        NotificationHelper.ensureChannels(this);

        setContentView(buildRoot());
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
        showPage(0);
        refreshPanel();
        refreshNotes();
        refreshReminders();
        updateNotificationState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        if (currentPage == 0) {
            refreshPanel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    public void onBackPressed() {
        if (currentPage != 0) {
            showPage(0);
        } else {
            super.onBackPressed();
        }
    }

    private void initPalette() {
        BG = getColor(R.color.bg);
        ELEVATED = getColor(R.color.bg_elevated);
        CARD = getColor(R.color.card);
        STROKE = getColor(R.color.card_stroke);
        TEXT = getColor(R.color.text);
        MUTED = getColor(R.color.text_muted);
        ACCENT = getColor(R.color.accent);
        GREEN = getColor(R.color.green);
        ORANGE = getColor(R.color.orange);
        RED = getColor(R.color.red);
    }

    private void loadAlertSettings() {
        alertsEnabled = prefs.getBoolean(K_ALERTS, true);
        alertLow = prefs.getInt(K_LOW, 20);
        alertHigh = prefs.getInt(K_HIGH, 90);
        alertTemp = prefs.getInt(K_TEMP, 45);
    }

    // ---------------------------------------------------------------- Iskelet

    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BG, Color.parseColor("#09111E")});
        root.setBackground(background);

        root.addView(buildAppBar());

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            if (v.getPaddingTop() != top || v.getPaddingBottom() != bottom) {
                v.setPadding(0, top, 0, bottom);
            }
            return insets;
        });
        root.requestApplyInsets();

        pageHost = new FrameLayout(this);
        root.addView(pageHost, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        panelScroll = buildPanelPage();
        toolsScroll = buildToolsPage();
        settingsScroll = buildSettingsPage();
        pageHost.addView(panelScroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        pageHost.addView(toolsScroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        pageHost.addView(settingsScroll, new FrameLayout.LayoutParams(MATCH, MATCH));

        root.addView(buildBottomNav());
        return root;
    }

    private View buildAppBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), dp(14), dp(18), dp(10));

        TextView logo = text("Y", 20, ACCENT, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(bg(Color.parseColor("#123044"), 13, 0));
        bar.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titlesLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        titlesLp.leftMargin = dp(11);
        TextView title = text("YEK PANEL", 17, TEXT, true);
        title.setLetterSpacing(0.08f);
        TextView subtitle = text("MOBİL SİSTEM PANELİ", 9, MUTED, false);
        subtitle.setLetterSpacing(0.16f);
        subtitle.setPadding(0, dp(3), 0, 0);
        titles.addView(title);
        titles.addView(subtitle);
        bar.addView(titles, titlesLp);

        ImageButton refresh = new ImageButton(this);
        refresh.setImageResource(R.drawable.ic_refresh);
        refresh.setColorFilter(ACCENT);
        refresh.setPadding(dp(10), dp(10), dp(10), dp(10));
        refresh.setBackground(ripple(bg(Color.parseColor("#14243A"), 12, STROKE)));
        refresh.setContentDescription("Yenile");
        refresh.setOnClickListener(v -> refreshAll());
        bar.addView(refresh, new LinearLayout.LayoutParams(dp(42), dp(42)));

        return bar;
    }

    private View buildBottomNav() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(ELEVATED);

        View line = new View(this);
        line.setBackgroundColor(STROKE);
        outer.addView(line, new LinearLayout.LayoutParams(MATCH, dp(1)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), dp(6), dp(8), dp(8));
        outer.addView(row);

        String[] labels = {"PANEL", "ARAÇLAR", "AYARLAR"};
        int[] icons = {R.drawable.ic_nav_panel, R.drawable.ic_nav_tools, R.drawable.ic_nav_info};
        for (int i = 0; i < labels.length; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(8), dp(8), dp(8), dp(6));
            item.setContentDescription(labels[i] + " sekmesi");

            ImageView icon = new ImageView(this);
            icon.setImageResource(icons[i]);
            icon.setColorFilter(MUTED);
            item.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

            TextView label = text(labels[i], 10, MUTED, true);
            label.setLetterSpacing(0.06f);
            label.setPadding(0, dp(4), 0, 0);
            item.addView(label);

            final int index = i;
            item.setOnClickListener(v -> showPage(index));
            row.addView(item, new LinearLayout.LayoutParams(0, WRAP, 1f));

            navItems[i] = item;
            navIcons[i] = icon;
            navLabels[i] = label;
        }
        return outer;
    }

    private void showPage(int index) {
        currentPage = index;
        panelScroll.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        toolsScroll.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        settingsScroll.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < navItems.length; i++) {
            boolean selected = i == index;
            navIcons[i].setColorFilter(selected ? ACCENT : MUTED);
            navLabels[i].setTextColor(selected ? ACCENT : MUTED);
            navItems[i].setBackground(selected ? bg(Color.parseColor("#141F33"), 14, 0) : null);
        }

        if (index == 0) {
            refreshPanel();
        } else if (index == 1) {
            refreshToolsNetwork();
            refreshNotes();
            refreshReminders();
        } else {
            updateNotificationState();
        }
    }

    private void refreshAll() {
        refreshPanel();
        if (currentPage == 1) {
            refreshToolsNetwork();
            refreshNotes();
            refreshReminders();
        }
        updateNotificationState();
        Toast.makeText(this, "Veriler güncellendi", Toast.LENGTH_SHORT).show();
    }

    private ScrollView pageScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(4), dp(18), dp(26));
        scroll.addView(content, new ScrollView.LayoutParams(MATCH, WRAP));
        scroll.setTag(content);
        return scroll;
    }

    private LinearLayout content(ScrollView scroll) {
        return (LinearLayout) scroll.getTag();
    }

    // ---------------------------------------------------------------- Panel

    private ScrollView buildPanelPage() {
        ScrollView scroll = pageScroll();
        LinearLayout c = content(scroll);

        stateChip = chip("HAZIR", GREEN);
        c.addView(sectionHeader("CANLI DURUM", stateChip));

        LinearLayout hero = card();
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        batteryRing = new BatteryRingView(this);
        hero.addView(batteryRing, new LinearLayout.LayoutParams(dp(116), dp(116)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        infoLp.leftMargin = dp(16);
        info.addView(labelText("PİL DURUMU"));

        batteryStatus = valueText("—", 20, TEXT, true);
        batteryStatus.setPadding(0, dp(4), 0, 0);
        info.addView(batteryStatus);

        batteryMeta = text("—", 12, MUTED, false);
        batteryMeta.setPadding(0, dp(2), 0, 0);
        info.addView(batteryMeta);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(9), 0, 0);
        batteryTempChip = chip("— °C", ORANGE);
        batteryHealthChip = chip("Sağlık: —", GREEN);
        chips.addView(batteryTempChip);
        LinearLayout.LayoutParams healthLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        healthLp.leftMargin = dp(6);
        chips.addView(batteryHealthChip, healthLp);
        info.addView(chips);

        batteryHint = text("", 10, MUTED, false);
        batteryHint.setPadding(0, dp(9), 0, 0);
        info.addView(batteryHint);

        hero.addView(info, infoLp);
        addCard(c, hero);

        LinearLayout cpuCard = card();
        cpuChip = chip("— ÇEKİRDEK", ACCENT);
        cpuCard.addView(cardHeader("İŞLEMCİ", cpuChip));

        LinearLayout cpuBig = new LinearLayout(this);
        cpuBig.setOrientation(LinearLayout.HORIZONTAL);
        cpuBig.setGravity(Gravity.BOTTOM);
        cpuBig.setPadding(0, dp(8), 0, 0);
        cpuValue = valueText("—", 30, TEXT, true);
        cpuBig.addView(cpuValue);
        cpuMeta = text("", 12, MUTED, false);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        metaLp.leftMargin = dp(8);
        metaLp.bottomMargin = dp(4);
        cpuBig.addView(cpuMeta, metaLp);
        View spacer = new View(this);
        cpuBig.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView loadLabel = text("YÜK", 9, MUTED, true);
        loadLabel.setLetterSpacing(0.12f);
        loadLabel.setPadding(0, 0, 0, dp(5));
        cpuBig.addView(loadLabel);
        cpuCard.addView(cpuBig);

        cpuSpark = new SparkView(this);
        LinearLayout.LayoutParams sparkLp = new LinearLayout.LayoutParams(MATCH, dp(52));
        sparkLp.topMargin = dp(10);
        cpuCard.addView(cpuSpark, sparkLp);

        cpuDetail = text("", 10, MUTED, false);
        cpuDetail.setPadding(0, dp(8), 0, 0);
        cpuCard.addView(cpuDetail);
        addCard(c, cpuCard);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout ramCard = card();
        ramCard.addView(labelText("BELLEK"));
        ramValue = valueText("—", 26, TEXT, true);
        ramValue.setPadding(0, dp(6), 0, 0);
        ramCard.addView(ramValue);
        ramSub = text("—", 10, MUTED, false);
        ramSub.setPadding(0, dp(2), 0, 0);
        ramCard.addView(ramSub);
        ramMeter = new MeterView(this);
        LinearLayout.LayoutParams meterLp = new LinearLayout.LayoutParams(MATCH, dp(7));
        meterLp.topMargin = dp(10);
        ramCard.addView(ramMeter, meterLp);

        LinearLayout storageCard = card();
        storageCard.addView(labelText("DEPOLAMA"));
        storageValue = valueText("—", 26, TEXT, true);
        storageValue.setPadding(0, dp(6), 0, 0);
        storageCard.addView(storageValue);
        storageSub = text("—", 10, MUTED, false);
        storageSub.setPadding(0, dp(2), 0, 0);
        storageCard.addView(storageSub);
        storageMeter = new MeterView(this);
        LinearLayout.LayoutParams meter2Lp = new LinearLayout.LayoutParams(MATCH, dp(7));
        meter2Lp.topMargin = dp(10);
        storageCard.addView(storageMeter, meter2Lp);

        LinearLayout.LayoutParams ramLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        ramLp.rightMargin = dp(5);
        grid.addView(ramCard, ramLp);
        LinearLayout.LayoutParams storageLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        storageLp.leftMargin = dp(5);
        grid.addView(storageCard, storageLp);
        addCard(c, grid);

        LinearLayout netCard = card();
        netChip = chip("—", GREEN);
        netCard.addView(cardHeader("AĞ", netChip));
        netType = valueText("—", 22, TEXT, true);
        netType.setPadding(0, dp(7), 0, 0);
        netCard.addView(netType);
        netIp = text("—", 12, MUTED, false);
        netIp.setPadding(0, dp(2), 0, 0);
        netCard.addView(netIp);
        TextView netHint = text("Gecikme ölçmek için Araçlar → Ağ testi", 10, MUTED, false);
        netHint.setPadding(0, dp(10), 0, 0);
        netCard.addView(netHint);
        addCard(c, netCard);

        LinearLayout sysCard = card();
        sysCard.addView(cardHeader("SİSTEM", null));
        sysDevice = rowValue();
        addInfoRow(sysCard, "Cihaz", sysDevice);
        sysAndroid = rowValue();
        addInfoRow(sysCard, "Android", sysAndroid);
        sysUptime = rowValue();
        addInfoRow(sysCard, "Çalışma süresi", sysUptime);
        sysChip = rowValue();
        addInfoRow(sysCard, "Yonga", sysChip);
        addCard(c, sysCard);

        TextView tip = text("Veriler 2 saniyede bir yenilenir · tümü cihazda kalır", 10, MUTED, false);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(0, dp(2), 0, 0);
        c.addView(tip);

        return scroll;
    }

    private void refreshPanel() {
        DeviceStats.Snapshot st = DeviceStats.read(this);
        stats = st;

        batteryRing.setData(st.batteryPercent, st.charging);
        String pct = st.batteryPercent >= 0 ? "%" + st.batteryPercent : "—";
        if (st.charging) {
            batteryStatus.setText(st.batteryPercent >= 100 ? "TAM ŞARJ" : "ŞARJ OLUYOR");
            batteryStatus.setTextColor(GREEN);
        } else {
            batteryStatus.setText("PİL MODU");
            batteryStatus.setTextColor(TEXT);
        }
        batteryMeta.setText(pct + (st.batteryTempC > 0
                ? String.format(Locale.getDefault(), " · %.1f °C", st.batteryTempC) : ""));
        applyChip(batteryTempChip, st.batteryTempC > 0
                ? String.format(Locale.getDefault(), "%.1f °C", st.batteryTempC) : "— °C",
                st.batteryTempC >= alertTemp ? RED : ORANGE);
        applyChip(batteryHealthChip, "Sağlık: " + st.batteryHealth,
                "—".equals(st.batteryHealth) ? MUTED : GREEN);
        batteryHint.setText("Uyarı eşiği: %" + alertLow + " · "
                + (alertsEnabled ? "uyarılar açık" : "uyarılar kapalı"));

        cpuChip.setText(st.cores + " ÇEKİRDEK");
        if (st.cpuLoad >= 0) {
            cpuValue.setText("%" + st.cpuLoad);
            cpuSpark.push(st.cpuLoad);
            cpuMeta.setText(st.cpuFreqGhz > 0
                    ? String.format(Locale.getDefault(), "%.2f GHz", st.cpuFreqGhz) : "");
        } else {
            cpuValue.setText("—");
            cpuMeta.setText(st.load1 >= 0
                    ? String.format(Locale.getDefault(), "yük ort. %.2f", st.load1) : "yük kısıtlı");
        }
        cpuDetail.setText(st.cores + " çekirdek"
                + (st.cpuFreqGhz > 0
                ? String.format(Locale.getDefault(), " · ortalama %.2f GHz", st.cpuFreqGhz) : "")
                + (st.soc == null || st.soc.isEmpty() ? "" : " · " + st.soc));

        ramValue.setText(st.ramPercent >= 0 ? "%" + st.ramPercent : "—");
        ramSub.setText(st.ramPercent >= 0
                ? DeviceStats.gb(st.ramTotal - st.ramAvail) + " / " + DeviceStats.gb(st.ramTotal)
                : "—");
        ramMeter.setProgress(Math.max(0, st.ramPercent),
                st.ramPercent >= 85 ? ORANGE : ACCENT);

        storageValue.setText(st.storagePercent >= 0 ? "%" + st.storagePercent : "—");
        storageSub.setText(st.storagePercent >= 0
                ? DeviceStats.gb(st.storageFree) + " boş" : "—");
        storageMeter.setProgress(Math.max(0, st.storagePercent),
                st.storagePercent >= 85 ? ORANGE : ACCENT);

        netType.setText(st.connType);
        netIp.setText(st.localIp);
        applyChip(netChip, st.connected ? "BAĞLI" : "ÇEVRİMDIŞI", st.connected ? GREEN : RED);

        sysDevice.setText(deviceName(st));
        sysAndroid.setText(st.android + " · API " + st.sdk);
        sysUptime.setText(DeviceStats.uptime(st.uptimeMs));
        sysChip.setText(st.soc == null || st.soc.isEmpty() ? "—" : st.soc);

        String state;
        int stateColor;
        if (st.batteryTempC > 0 && st.batteryTempC >= alertTemp) {
            state = "SICAKLIK YÜKSEK";
            stateColor = ORANGE;
        } else if (alertsEnabled && st.batteryPercent >= 0
                && st.batteryPercent <= alertLow && !st.charging) {
            state = "PİL DÜŞÜK";
            stateColor = RED;
        } else if (st.charging) {
            state = "ŞARJ OLUYOR";
            stateColor = GREEN;
        } else {
            state = "SİSTEM İYİ";
            stateColor = GREEN;
        }
        applyChip(stateChip, state, stateColor);

        if (alertsEnabled) {
            evaluateAlerts(st);
        }
    }

    private void evaluateAlerts(DeviceStats.Snapshot st) {
        long now = System.currentTimeMillis();
        if (st.batteryPercent >= 0 && st.batteryPercent <= alertLow && !st.charging
                && now - prefs.getLong("last_low", 0) > ALERT_COOLDOWN) {
            prefs.edit().putLong("last_low", now).apply();
            NotificationHelper.notify(this, 201, NotificationHelper.CHANNEL_ALERTS,
                    "Pil düşük", "Pil %" + st.batteryPercent
                            + " seviyesinde. Şarj etmeyi düşünün.");
        }
        if (st.charging && st.batteryPercent >= alertHigh
                && now - prefs.getLong("last_high", 0) > ALERT_COOLDOWN) {
            prefs.edit().putLong("last_high", now).apply();
            NotificationHelper.notify(this, 202, NotificationHelper.CHANNEL_ALERTS,
                    "Pil seviyesi yüksek", "Pil %" + st.batteryPercent
                            + ". Şarjdan ayırmak pil ömrüne iyi gelir.");
        }
        if (st.batteryTempC > 0 && st.batteryTempC >= alertTemp
                && now - prefs.getLong("last_temp", 0) > ALERT_COOLDOWN) {
            prefs.edit().putLong("last_temp", now).apply();
            NotificationHelper.notify(this, 203, NotificationHelper.CHANNEL_ALERTS,
                    "Cihaz ısınıyor",
                    String.format(Locale.getDefault(),
                            "Pil sıcaklığı %.1f °C. Ağır işlemleri durdurun.", st.batteryTempC));
        }
    }

    // ---------------------------------------------------------------- Araçlar

    private ScrollView buildToolsPage() {
        ScrollView scroll = pageScroll();
        LinearLayout c = content(scroll);

        c.addView(sectionHeader("ARAÇLAR", chip("3 ARAÇ", ACCENT)));

        LinearLayout netCard = card();
        netCard.addView(cardHeader("AĞ TESTİ", chip("ICMP", ACCENT)));
        TextView netDesc = text("Bağlantı gecikmesini ölçün. Sonuçlar cihazda kalır.", 11, MUTED, false);
        netDesc.setPadding(0, dp(6), 0, 0);
        netCard.addView(netDesc);

        toolsNetText = text("—", 12, TEXT, false);
        toolsNetText.setPadding(0, dp(8), 0, 0);
        netCard.addView(toolsNetText);

        LinearLayout targets = new LinearLayout(this);
        targets.setOrientation(LinearLayout.HORIZONTAL);
        targets.setPadding(0, dp(10), 0, 0);
        for (int i = 0; i < pingLabels.length; i++) {
            final int index = i;
            TextView target = chip(pingLabels[i], i == 0 ? ACCENT : MUTED);
            target.setOnClickListener(v -> selectPingTarget(index));
            target.setContentDescription(pingLabels[i] + " hedefi");
            pingChips[i] = target;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            if (i > 0) {
                lp.leftMargin = dp(6);
            }
            targets.addView(target, lp);
        }
        netCard.addView(targets);

        pingButton = button("PİNG TESTİ", true);
        LinearLayout.LayoutParams pingLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        pingLp.topMargin = dp(12);
        pingButton.setOnClickListener(v -> runPing());
        netCard.addView(pingButton, pingLp);

        pingProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        pingProgress.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        pingProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        progressLp.gravity = Gravity.CENTER_HORIZONTAL;
        progressLp.topMargin = dp(8);
        netCard.addView(pingProgress, progressLp);

        pingResult = text("Hazır. Hedef seçip testi başlatın.", 12, MUTED, false);
        pingResult.setPadding(0, dp(8), 0, 0);
        pingResult.setLineSpacing(dp(3), 1f);
        netCard.addView(pingResult);
        addCard(c, netCard);

        LinearLayout notesCard = card();
        noteCountChip = chip("0 NOT", ACCENT);
        notesCard.addView(cardHeader("NOTLAR", noteCountChip));
        TextView noteDesc = text("Notlar yalnızca bu cihazda saklanır.", 11, MUTED, false);
        noteDesc.setPadding(0, dp(6), 0, 0);
        notesCard.addView(noteDesc);

        noteTitleInput = input("Başlık", false);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        titleLp.topMargin = dp(10);
        notesCard.addView(noteTitleInput, titleLp);

        noteBodyInput = input("Notunuz…", true);
        noteBodyInput.setMinHeight(dp(96));
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        bodyLp.topMargin = dp(8);
        notesCard.addView(noteBodyInput, bodyLp);

        LinearLayout noteActions = new LinearLayout(this);
        noteActions.setOrientation(LinearLayout.HORIZONTAL);
        noteActions.setPadding(0, dp(10), 0, 0);
        noteSaveButton = button("KAYDET", true);
        noteSaveButton.setOnClickListener(v -> saveNote());
        noteClearButton = button("TEMİZLE", false);
        noteClearButton.setOnClickListener(v -> resetNoteEditor());
        noteActions.addView(noteSaveButton, new LinearLayout.LayoutParams(0, WRAP, 1f));
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        clearLp.leftMargin = dp(8);
        noteActions.addView(noteClearButton, clearLp);
        notesCard.addView(noteActions);

        LinearLayout exportRow = new LinearLayout(this);
        exportRow.setOrientation(LinearLayout.HORIZONTAL);
        exportRow.setPadding(0, dp(8), 0, 0);
        TextView exportTxt = button("TXT AKTAR", false);
        exportTxt.setOnClickListener(v -> exportNotes(false));
        TextView exportMd = button("MD AKTAR", false);
        exportMd.setOnClickListener(v -> exportNotes(true));
        TextView share = button("PAYLAŞ", false);
        share.setOnClickListener(v -> shareNotes());
        exportRow.addView(exportTxt, new LinearLayout.LayoutParams(0, WRAP, 1f));
        LinearLayout.LayoutParams mdLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        mdLp.leftMargin = dp(6);
        exportRow.addView(exportMd, mdLp);
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        shareLp.leftMargin = dp(6);
        exportRow.addView(share, shareLp);
        notesCard.addView(exportRow);

        noteList = new LinearLayout(this);
        noteList.setOrientation(LinearLayout.VERTICAL);
        noteList.setPadding(0, dp(6), 0, 0);
        notesCard.addView(noteList);
        addCard(c, notesCard);

        LinearLayout remCard = card();
        reminderCountChip = chip("0 GÜNLÜK", ORANGE);
        remCard.addView(cardHeader("HATIRLATICI", reminderCountChip));
        TextView remDesc = text("Her gün seçtiğiniz saatte bildirim gönderir.", 11, MUTED, false);
        remDesc.setPadding(0, dp(6), 0, 0);
        remCard.addView(remDesc);

        reminderTimeButton = text(fmtTime(reminderHour, reminderMinute), 26, ACCENT, true);
        reminderTimeButton.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        reminderTimeButton.setGravity(Gravity.CENTER);
        reminderTimeButton.setBackground(ripple(bg(Color.parseColor("#0E1728"), 12, STROKE)));
        reminderTimeButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        reminderTimeButton.setContentDescription("Hatırlatıcı saatini seç");
        reminderTimeButton.setOnClickListener(v -> pickReminderTime());
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        timeLp.topMargin = dp(10);
        remCard.addView(reminderTimeButton, timeLp);

        reminderLabelInput = input("Hatırlatıcı adı (örn. Su iç)", false);
        LinearLayout.LayoutParams remLabelLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        remLabelLp.topMargin = dp(8);
        remCard.addView(reminderLabelInput, remLabelLp);

        TextView addReminder = button("HATIRLATICI EKLE", true);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        addLp.topMargin = dp(10);
        addReminder.setOnClickListener(v -> addReminder());
        remCard.addView(addReminder, addLp);

        reminderList = new LinearLayout(this);
        reminderList.setOrientation(LinearLayout.VERTICAL);
        reminderList.setPadding(0, dp(6), 0, 0);
        remCard.addView(reminderList);
        addCard(c, remCard);

        return scroll;
    }

    private void refreshToolsNetwork() {
        if (toolsNetText == null) {
            return;
        }
        DeviceStats.Snapshot st = DeviceStats.read(this);
        toolsNetText.setText(st.connType + " · " + st.localIp + " · "
                + (st.connected ? "bağlı" : "çevrimdışı"));
        toolsNetText.setTextColor(st.connected ? TEXT : RED);
    }

    private void selectPingTarget(int index) {
        pingTargetIndex = index;
        for (int i = 0; i < pingChips.length; i++) {
            applyChip(pingChips[i], pingLabels[i], i == index ? ACCENT : MUTED);
        }
    }

    private void runPing() {
        if (pinging) {
            return;
        }
        pinging = true;
        pingButton.setEnabled(false);
        pingButton.setAlpha(0.6f);
        pingProgress.setVisibility(View.VISIBLE);
        pingResult.setTextColor(MUTED);
        pingResult.setText("Test ediliyor…");
        final String target = pingTargets[pingTargetIndex];

        new Thread(() -> {
            final PingOutcome outcome = ping(target);
            runOnUiThread(() -> {
                pinging = false;
                pingButton.setEnabled(true);
                pingButton.setAlpha(1f);
                pingProgress.setVisibility(View.GONE);
                pingResult.setText(outcome.text);
                pingResult.setTextColor(outcome.color);
            });
        }, "yek-ping").start();
    }

    private static final class PingOutcome {
        final String text;
        final int color;

        PingOutcome(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    private PingOutcome ping(String target) {
        List<Double> times = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"/system/bin/ping", "-c", "3", "-W", "2", target});
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroy();
            }
            Matcher matcher = Pattern.compile("time=([0-9.]+)\\s*ms").matcher(output);
            while (matcher.find()) {
                times.add(Double.parseDouble(matcher.group(1)));
            }
        } catch (Exception ignored) {
        }

        if (times.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                long start = System.nanoTime();
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(target, 53), 2000);
                    times.add((System.nanoTime() - start) / 1_000_000.0);
                } catch (Exception ignored) {
                    break;
                }
            }
        }

        if (times.isEmpty()) {
            return new PingOutcome("Test başarısız. Bağlantıyı veya hedefi kontrol edin.", RED);
        }

        double min = Double.MAX_VALUE;
        double max = 0;
        double sum = 0;
        for (double value : times) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }
        double avg = sum / times.size();
        int color = avg < 60 ? GREEN : (avg < 150 ? ORANGE : RED);
        String text = String.format(Locale.getDefault(),
                "%d/3 yanıt · ortalama %.0f ms%nEn iyi %.0f ms · en kötü %.0f ms",
                times.size(), avg, min, max);
        return new PingOutcome(text, color);
    }

    // ---------------------------------------------------------------- Notlar

    private void saveNote() {
        String title = noteTitleInput.getText().toString().trim();
        String body = noteBodyInput.getText().toString().trim();
        if (title.isEmpty() && body.isEmpty()) {
            Toast.makeText(this, "Boş not kaydedilmez", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editingNoteId > 0) {
            noteStore.update(editingNoteId, title, body);
            Toast.makeText(this, "Not güncellendi", Toast.LENGTH_SHORT).show();
        } else {
            noteStore.add(title, body);
            Toast.makeText(this, "Not kaydedildi", Toast.LENGTH_SHORT).show();
        }
        resetNoteEditor();
        refreshNotes();
    }

    private void resetNoteEditor() {
        editingNoteId = -1;
        if (noteTitleInput != null) {
            noteTitleInput.setText("");
        }
        if (noteBodyInput != null) {
            noteBodyInput.setText("");
        }
        if (noteSaveButton != null) {
            noteSaveButton.setText("KAYDET");
        }
        if (noteClearButton != null) {
            noteClearButton.setText("TEMİZLE");
        }
    }

    private void loadNoteForEdit(NoteStore.Note note) {
        editingNoteId = note.id;
        noteTitleInput.setText(note.title);
        noteBodyInput.setText(note.body);
        noteSaveButton.setText("GÜNCELLE");
        noteClearButton.setText("VAZGEÇ");
        noteTitleInput.requestFocus();
    }

    private void refreshNotes() {
        if (noteList == null) {
            return;
        }
        List<NoteStore.Note> notes = noteStore.all();
        noteCountChip.setText(notes.size() + " NOT");
        noteList.removeAllViews();
        if (notes.isEmpty()) {
            TextView empty = text("Henüz not yok. İlk notunuzu ekleyin.", 11, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(14), 0, dp(8));
            noteList.addView(empty);
            return;
        }
        for (NoteStore.Note note : notes) {
            noteList.addView(noteRow(note));
        }
    }

    private View noteRow(NoteStore.Note note) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(bg(Color.parseColor("#0E1728"), 12, STROKE));
        box.setPadding(dp(12), dp(11), dp(12), dp(10));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        boxLp.topMargin = dp(8);
        box.setLayoutParams(boxLp);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(note.title, 13, TEXT, true);
        title.setMaxLines(2);
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView date = text(new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                .format(new Date(note.updatedAt)), 9, MUTED, false);
        head.addView(date);
        box.addView(head);

        if (!note.body.isEmpty()) {
            TextView preview = text(note.body.replace("\n", " "), 11, MUTED, false);
            preview.setMaxLines(3);
            preview.setPadding(0, dp(5), 0, 0);
            box.addView(preview);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);
        TextView copy = actionChip("KOPYALA", ACCENT);
        copy.setOnClickListener(v -> {
            ClipboardManager manager =
                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText(
                        "YEK Panel notu", note.title + "\n\n" + note.body));
                Toast.makeText(this, "Not kopyalandı", Toast.LENGTH_SHORT).show();
            }
        });
        TextView edit = actionChip("DÜZENLE", ACCENT);
        edit.setOnClickListener(v -> loadNoteForEdit(note));
        TextView delete = actionChip("SİL", RED);
        delete.setOnClickListener(v -> confirmDeleteNote(note));
        actions.addView(copy);
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        editLp.leftMargin = dp(6);
        actions.addView(edit, editLp);
        LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        deleteLp.leftMargin = dp(6);
        actions.addView(delete, deleteLp);
        box.addView(actions);

        return box;
    }

    private void confirmDeleteNote(NoteStore.Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Not silinsin mi?")
                .setMessage(note.title)
                .setPositiveButton("Sil", (dialog, which) -> {
                    noteStore.delete(note.id);
                    if (editingNoteId == note.id) {
                        resetNoteEditor();
                    }
                    refreshNotes();
                    Toast.makeText(this, "Not silindi", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void exportNotes(boolean markdown) {
        List<NoteStore.Note> notes = noteStore.all();
        if (notes.isEmpty()) {
            Toast.makeText(this, "Dışa aktarılacak not yok", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingExport = markdown ? noteStore.exportMarkdown() : noteStore.exportText();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE,
                markdown ? "yek-panel-notlar.md" : "yek-panel-notlar.txt");
        startActivityForResult(intent, markdown ? REQ_EXPORT_MD : REQ_EXPORT_TXT);
    }

    private void shareNotes() {
        List<NoteStore.Note> notes = noteStore.all();
        if (notes.isEmpty()) {
            Toast.makeText(this, "Paylaşılacak not yok", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "YEK Panel notları");
        send.putExtra(Intent.EXTRA_TEXT, noteStore.exportText());
        startActivity(Intent.createChooser(send, "Notları paylaş"));
    }

    // ---------------------------------------------------------------- Hatırlatıcı

    private void pickReminderTime() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            reminderHour = hourOfDay;
            reminderMinute = minute;
            reminderTimeButton.setText(fmtTime(reminderHour, reminderMinute));
        }, reminderHour, reminderMinute, true).show();
    }

    private void addReminder() {
        if (!NotificationHelper.canNotify(this)) {
            ensureNotifications();
        }
        String label = reminderLabelInput.getText().toString().trim();
        ReminderStore.Reminder reminder =
                reminderStore.add(label, reminderHour, reminderMinute);
        ReminderStore.schedule(this, reminder);
        reminderLabelInput.setText("");
        refreshReminders();
        Toast.makeText(this, "Her gün " + fmtTime(reminder.hour, reminder.minute)
                + " · " + reminder.label, Toast.LENGTH_LONG).show();
    }

    private void refreshReminders() {
        if (reminderList == null) {
            return;
        }
        List<ReminderStore.Reminder> reminders = reminderStore.all();
        int active = 0;
        for (ReminderStore.Reminder reminder : reminders) {
            if (reminder.enabled) {
                active++;
            }
        }
        reminderCountChip.setText(active + " GÜNLÜK");
        reminderList.removeAllViews();
        if (reminders.isEmpty()) {
            TextView empty = text("Henüz hatırlatıcı yok.", 11, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(14), 0, dp(8));
            reminderList.addView(empty);
            return;
        }
        for (ReminderStore.Reminder reminder : reminders) {
            reminderList.addView(reminderRow(reminder));
        }
    }

    private View reminderRow(ReminderStore.Reminder reminder) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(bg(Color.parseColor("#0E1728"), 12, STROKE));
        box.setPadding(dp(12), dp(10), dp(12), dp(9));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        boxLp.topMargin = dp(8);
        box.setLayoutParams(boxLp);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView time = text(fmtTime(reminder.hour, reminder.minute), 19, ACCENT, true);
        time.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        head.addView(time, new LinearLayout.LayoutParams(0, WRAP, 1f));

        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setChecked(reminder.enabled);
        toggle.setThumbTintList(ColorStateList.valueOf(ACCENT));
        toggle.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#1E2A44")));
        toggle.setContentDescription(reminder.label + " hatırlatıcısı");
        toggle.setOnCheckedChangeListener((button, checked) -> {
            reminderStore.setEnabled(reminder.id, checked);
            if (checked) {
                ReminderStore.schedule(this, reminder);
                if (!NotificationHelper.canNotify(this)) {
                    ensureNotifications();
                }
            } else {
                ReminderStore.cancel(this, reminder.id);
            }
            refreshReminders();
        });
        head.addView(toggle);
        box.addView(head);

        TextView label = text(reminder.label, 12, TEXT, false);
        label.setPadding(0, dp(2), 0, 0);
        box.addView(label);

        LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        TextView meta = text(reminder.enabled ? "Her gün · bildirim açık" : "Duraklatıldı",
                9, MUTED, false);
        foot.addView(meta, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView delete = actionChip("SİL", RED);
        delete.setOnClickListener(v -> confirmDeleteReminder(reminder));
        foot.addView(delete);
        box.addView(foot);

        return box;
    }

    private void confirmDeleteReminder(ReminderStore.Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle("Hatırlatıcı silinsin mi?")
                .setMessage(fmtTime(reminder.hour, reminder.minute) + " · " + reminder.label)
                .setPositiveButton("Sil", (dialog, which) -> {
                    ReminderStore.cancel(this, reminder.id);
                    reminderStore.delete(reminder.id);
                    refreshReminders();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    // ---------------------------------------------------------------- Ayarlar

    private ScrollView buildSettingsPage() {
        ScrollView scroll = pageScroll();
        LinearLayout c = content(scroll);

        c.addView(sectionHeader("AYARLAR", chip("v" + VERSION, GREEN)));

        LinearLayout alertsCard = card();
        alertsCard.addView(cardHeader("SİSTEM UYARILARI", null));

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        switchRow.setPadding(0, dp(6), 0, 0);
        TextView switchLabel = text("Uyarıları etkinleştir", 13, TEXT, false);
        switchRow.addView(switchLabel, new LinearLayout.LayoutParams(0, WRAP, 1f));
        alertsSwitch = new Switch(this);
        alertsSwitch.setShowText(false);
        alertsSwitch.setChecked(alertsEnabled);
        alertsSwitch.setThumbTintList(ColorStateList.valueOf(ACCENT));
        alertsSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#1E2A44")));
        switchRow.addView(alertsSwitch);
        alertsCard.addView(switchRow);

        TextView alertsDesc = text("Panel açıkken pil ve sıcaklık eşiklerini izler. "
                + "Bildirim için izin gerekir.", 10, MUTED, false);
        alertsDesc.setPadding(0, dp(4), 0, dp(4));
        alertsCard.addView(alertsDesc);

        lowSeek = addSlider(alertsCard, "Pil alt eşiği", 5, 50, alertLow, "%", value -> {
            alertLow = value;
            prefs.edit().putInt(K_LOW, value).apply();
            if (batteryHint != null) {
                batteryHint.setText("Uyarı eşiği: %" + alertLow + " · "
                        + (alertsEnabled ? "uyarılar açık" : "uyarılar kapalı"));
            }
        });
        highSeek = addSlider(alertsCard, "Pil üst eşiği", 70, 100, alertHigh, "%", value -> {
            alertHigh = value;
            prefs.edit().putInt(K_HIGH, value).apply();
        });
        tempSeek = addSlider(alertsCard, "Sıcaklık eşiği", 35, 55, alertTemp, " °C", value -> {
            alertTemp = value;
            prefs.edit().putInt(K_TEMP, value).apply();
        });

        TextView cooldown = text("Uyarılar arasında 30 dakika bekleme vardır.", 10, MUTED, false);
        cooldown.setPadding(0, dp(4), 0, 0);
        alertsCard.addView(cooldown);
        addCard(c, alertsCard);

        alertsSwitch.setOnCheckedChangeListener((button, checked) -> {
            alertsEnabled = checked;
            prefs.edit().putBoolean(K_ALERTS, checked).apply();
            if (checked) {
                ensureNotifications();
            }
            if (batteryHint != null) {
                batteryHint.setText("Uyarı eşiği: %" + alertLow + " · "
                        + (checked ? "uyarılar açık" : "uyarılar kapalı"));
            }
        });

        LinearLayout dataCard = card();
        dataCard.addView(cardHeader("VERİLER", null));
        TextView dataText = text("Notlar, hatırlatıcılar ve ayarlar yalnızca bu cihazda saklanır. "
                + "Uygulama kişisel verilerinizi bir sunucuya göndermez.", 11, MUTED, false);
        dataText.setPadding(0, dp(6), 0, 0);
        dataText.setLineSpacing(dp(3), 1f);
        dataCard.addView(dataText);

        notifState = text("", 11, MUTED, false);
        notifState.setPadding(0, dp(10), 0, 0);
        dataCard.addView(notifState);

        notifButton = button("BİLDİRİM İZNİNİ AÇ", false);
        LinearLayout.LayoutParams notifLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        notifLp.topMargin = dp(8);
        notifButton.setOnClickListener(v -> ensureNotifications());
        dataCard.addView(notifButton, notifLp);
        addCard(c, dataCard);

        LinearLayout aboutCard = card();
        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("Y", 20, ACCENT, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(bg(Color.parseColor("#123044"), 13, 0));
        brandRow.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        brandLp.leftMargin = dp(12);
        TextView brand = text("YEK PANEL", 16, TEXT, true);
        brand.setLetterSpacing(0.06f);
        TextView version = text("Android " + VERSION + " · YEK BİLGİSAYAR", 10, MUTED, false);
        brandText.addView(brand);
        brandText.addView(version);
        brandRow.addView(brandText, brandLp);
        aboutCard.addView(brandRow);

        TextView storeChip = chip("PLAY STORE YAKINDA", ORANGE);
        LinearLayout.LayoutParams storeLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        storeLp.topMargin = dp(12);
        aboutCard.addView(storeChip, storeLp);

        TextView aboutText = text("Masaüstü YEK Panel'in mobil yoldaşı. Pil, bellek, "
                + "depolama, işlemci ve ağ durumunu tek ekranda toplar.", 11, MUTED, false);
        aboutText.setPadding(0, dp(12), 0, dp(4));
        aboutText.setLineSpacing(dp(3), 1f);
        aboutCard.addView(aboutText);

        TextView versionRow = rowValue();
        addInfoRow(aboutCard, "Sürüm", versionRow);
        versionRow.setText(VERSION);
        TextView packageRow = rowValue();
        addInfoRow(aboutCard, "Paket", packageRow);
        packageRow.setText("com.yekbilgisayar.yekpanel");
        TextView developerRow = rowValue();
        addInfoRow(aboutCard, "Geliştirici", developerRow);
        developerRow.setText("YEK BİLGİSAYAR");
        addCard(c, aboutCard);

        TextView footer = text("YEK BİLGİSAYAR · 2026", 10, MUTED, true);
        footer.setLetterSpacing(0.08f);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(4), 0, 0);
        c.addView(footer);

        return scroll;
    }

    private SeekBar addSlider(LinearLayout parent, String title, int min, int max,
                              int current, String suffix, IntConsumer onChange) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, dp(10), 0, 0);
        TextView label = text(title, 12, TEXT, false);
        head.addView(label, new LinearLayout.LayoutParams(0, WRAP, 1f));
        final int[] valueHolder = {Math.max(min, Math.min(max, current))};
        TextView value = text(valueHolder[0] + suffix, 12, ACCENT, true);
        head.addView(value);
        parent.addView(head);

        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(valueHolder[0] - min);
        seek.setProgressTintList(ColorStateList.valueOf(ACCENT));
        seek.setThumbTintList(ColorStateList.valueOf(ACCENT));
        seek.setProgressBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1B2740")));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                valueHolder[0] = min + progress;
                value.setText(valueHolder[0] + suffix);
                onChange.accept(valueHolder[0]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        parent.addView(seek, new LinearLayout.LayoutParams(MATCH, WRAP));
        return seek;
    }

    private void ensureNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_NOTIFICATIONS);
        } else {
            Toast.makeText(this, "Bildirim izni açık", Toast.LENGTH_SHORT).show();
        }
        updateNotificationState();
    }

    private void updateNotificationState() {
        if (notifState == null || notifButton == null) {
            return;
        }
        boolean allowed = NotificationHelper.canNotify(this);
        notifState.setText(allowed
                ? "Bildirim izni: açık · uyarılar ve hatırlatıcılar gösterilebilir"
                : "Bildirim izni: kapalı · uyarılar gösterilemez");
        notifState.setTextColor(allowed ? GREEN : ORANGE);
        notifButton.setText(allowed ? "BİLDİRİM AYARLARI" : "BİLDİRİM İZNİNİ AÇ");
        notifButton.setOnClickListener(v -> {
            if (NotificationHelper.canNotify(this)) {
                Intent intent = new Intent();
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
                try {
                    startActivity(intent);
                } catch (Exception ignored) {
                }
            } else {
                ensureNotifications();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this, granted ? "Bildirim izni verildi"
                    : "Bildirim izni verilmedi. Uyarılar gösterilemez.",
                    Toast.LENGTH_LONG).show();
            updateNotificationState();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQ_EXPORT_TXT || requestCode == REQ_EXPORT_MD)
                && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try (OutputStream raw = getContentResolver().openOutputStream(uri, "wt")) {
                if (raw == null) {
                    throw new IllegalStateException("Çıktı akışı açılamadı");
                }
                try (OutputStreamWriter writer = new OutputStreamWriter(raw, "UTF-8")) {
                    writer.write(pendingExport);
                }
                Toast.makeText(this, "Notlar dışa aktarıldı", Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                Toast.makeText(this, "Dışa aktarma başarısız", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---------------------------------------------------------------- Yardımcılar

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(bold
                ? Typeface.create("sans-serif-medium", Typeface.BOLD)
                : Typeface.create("sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView labelText(String value) {
        TextView view = text(value, 10, MUTED, true);
        view.setLetterSpacing(0.13f);
        return view;
    }

    private TextView valueText(String value, float sp, int color, boolean bold) {
        TextView view = text(value, sp, color, bold);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return view;
    }

    private TextView rowValue() {
        TextView view = text("—", 12, TEXT, false);
        view.setGravity(Gravity.END);
        return view;
    }

    private TextView chip(String value, int color) {
        TextView view = text(value, 9, color, true);
        view.setLetterSpacing(0.05f);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(5), dp(8), dp(5));
        applyChip(view, value, color);
        return view;
    }

    private void applyChip(TextView view, String value, int color) {
        view.setText(value);
        view.setTextColor(color);
        view.setBackground(bg(withAlpha(color, 0x26), 999, withAlpha(color, 0x55)));
    }

    private TextView button(String label, boolean primary) {
        TextView view = text(label, 11, primary ? Color.parseColor("#06121F") : TEXT, true);
        view.setLetterSpacing(0.04f);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        view.setBackground(ripple(bg(primary ? ACCENT : Color.parseColor("#14243A"),
                12, primary ? 0 : STROKE)));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView actionChip(String label, int color) {
        TextView view = text(label, 9, color, true);
        view.setLetterSpacing(0.05f);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(6), dp(10), dp(6));
        view.setBackground(ripple(bg(withAlpha(color, 0x1F), 8, withAlpha(color, 0x55))));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private EditText input(String hint, boolean multiline) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setTextSize(13);
        view.setTextColor(TEXT);
        view.setHintTextColor(MUTED);
        view.setBackground(bg(Color.parseColor("#0E1728"), 12, STROKE));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        view.setIncludeFontPadding(false);
        if (multiline) {
            view.setGravity(Gravity.TOP | Gravity.START);
            view.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        } else {
            view.setSingleLine(true);
            view.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        return view;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(15), dp(16), dp(15));
        layout.setBackground(bg(CARD, 20, STROKE));
        return layout;
    }

    private void addCard(LinearLayout parent, View child) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(12);
        parent.addView(child, lp);
    }

    private LinearLayout sectionHeader(String title, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(10));
        TextView label = text(title, 10, ACCENT, true);
        label.setLetterSpacing(0.16f);
        row.addView(label);
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        if (right != null) {
            row.addView(right);
        }
        return row;
    }

    private LinearLayout cardHeader(String title, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(title, 10, MUTED, true);
        label.setLetterSpacing(0.14f);
        row.addView(label, new LinearLayout.LayoutParams(0, WRAP, 1f));
        if (right != null) {
            row.addView(right);
        }
        return row;
    }

    private void addInfoRow(LinearLayout parent, String label, TextView value) {
        parent.addView(divider());
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));
        TextView name = text(label, 12, MUTED, false);
        row.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));
        value.setTextSize(12);
        value.setTextColor(TEXT);
        value.setGravity(Gravity.END);
        value.setMaxLines(2);
        row.addView(value, new LinearLayout.LayoutParams(0, WRAP, 1.4f));
        parent.addView(row);
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(Color.parseColor("#16233A"));
        view.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(1)));
        return view;
    }

    private Drawable bg(int color, float radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private Drawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, null);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private String fmtTime(int hour, int minute) {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private String deviceName(DeviceStats.Snapshot snapshot) {
        String manufacturer = snapshot.manufacturer == null ? "" : snapshot.manufacturer;
        String model = snapshot.model == null ? "" : snapshot.model;
        if (manufacturer.isEmpty()) {
            return model;
        }
        String niceManufacturer = manufacturer.substring(0, 1).toUpperCase(Locale.getDefault())
                + manufacturer.substring(1);
        if (model.toLowerCase(Locale.getDefault())
                .startsWith(manufacturer.toLowerCase(Locale.getDefault()))) {
            return model;
        }
        return niceManufacturer + " " + model;
    }
}
