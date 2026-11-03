package com.yekbilgisayar.yekpanel;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
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
import android.window.OnBackInvokedDispatcher;

import com.yekbilgisayar.yekpanel.ui.BatteryRingView;
import com.yekbilgisayar.yekpanel.ui.MeterView;
import com.yekbilgisayar.yekpanel.ui.SparkView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements SensorEventListener {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static final int REQ_NOTIFICATIONS = 31;
    private static final int REQ_LOCAL_NETWORK = 32;
    private static final int REQ_EXPORT_TXT = 41;
    private static final int REQ_EXPORT_MD = 42;
    private static final int REQ_EXPORT_BACKUP = 51;
    private static final int REQ_IMPORT_BACKUP = 52;

    private static final long ALERT_COOLDOWN = 30L * 60L * 1000L;
    private static final long SERVER_CHECK_INTERVAL = 60L * 1000L;
    private static final String VERSION = "1.2.1";

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
    private ScrollView pcScroll;
    private ScrollView toolsScroll;
    private ScrollView settingsScroll;
    private int currentPage = 0;

    private final LinearLayout[] navItems = new LinearLayout[4];
    private final ImageView[] navIcons = new ImageView[4];
    private final TextView[] navLabels = new TextView[4];

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
    private boolean quietEnabled = false;
    private int quietStart = 23 * 60;
    private int quietEnd = 8 * 60;
    private int refreshSeconds = 2;

    private TextView batteryVoltageValue;
    private TextView batteryCurrentValue;
    private TextView batteryChargeValue;
    private TextView batteryTechValue;
    private TextView batterySourceValue;

    private TextView netSignalChip;
    private TextView netLinkChip;
    private TextView netGatewayValue;
    private TextView netDnsValue;
    private TextView externalIpValue;
    private boolean externalIpLoaded = false;
    private boolean externalIpLoading = false;

    private EndpointStore endpointStore;
    private final Map<Long, EndpointChecker.Result> serverResults = new HashMap<>();
    private LinearLayout serverList;
    private TextView serverSummary;
    private TextView serverCheckButton;
    private boolean checkingServers = false;
    private long lastServerCheckAt = 0;

    private EditText endpointNameInput;
    private EditText endpointUrlInput;
    private TextView endpointSummary;
    private LinearLayout endpointManageList;

    private TextView speedButton;
    private TextView speedResult;
    private ProgressBar speedProgress;
    private boolean speedRunning = false;

    private EditText diagHostInput;
    private EditText diagPortInput;
    private TextView diagResult;
    private boolean diagRunning = false;

    private SensorManager sensorManager;
    private Switch sensorSwitch;
    private LinearLayout sensorList;
    private final List<Sensor> activeSensors = new ArrayList<>();
    private final Map<Integer, TextView> sensorValueViews = new HashMap<>();
    private final Map<Integer, Long> sensorLastUi = new HashMap<>();
    private boolean sensorsRunning = false;

    private Switch quietSwitch;
    private TextView quietStartButton;
    private TextView quietEndButton;
    private final TextView[] refreshChips = new TextView[3];
    private TextView backupStatus;
    private String pendingRestoreJson = "";

    private EditText pcHostInput;
    private EditText pcPortInput;
    private EditText pcPinInput;
    private Switch pcAutoSwitch;
    private TextView pcConnectButton;
    private TextView pcDiscoverButton;
    private TextView pcStateChip;
    private TextView pcConnectState;
    private TextView pcName;
    private TextView pcMeta;
    private TextView pcCpuValue;
    private TextView pcRamValue;
    private TextView pcNetValue;
    private TextView pcLinkValue;
    private TextView pcTempValue;
    private TextView pcBatteryValue;
    private TextView pcUpdatedValue;
    private TextView pcIpValue;
    private MeterView pcCpuMeter;
    private MeterView pcRamMeter;
    private LinearLayout pcDiskList;
    private LinearLayout pcProcessList;
    private PcClient.Status pcStatus;
    private boolean pcConnected = false;
    private boolean pcConnecting = false;
    private boolean pcFetching = false;
    private boolean pcDiscovering = false;
    private int pcPendingAction = 0;
    private long pcNextRetryAt = 0;
    private String pcHost = "";
    private int pcPort = AppPrefs.DEFAULT_PC_PORT;
    private String pcPin = "";
    private boolean pcAuto = true;

    private TextView updateVersionValue;
    private TextView updateStatus;
    private TextView updateCheckButton;
    private TextView updateInstallButton;
    private TextView updateLastValue;
    private TextView updateNotesButton;
    private Switch updateAutoSwitch;
    private ProgressBar updateProgress;
    private boolean updateChecking = false;
    private boolean updateDownloading = false;
    private long updateDownloadId = -1;
    private UpdateChecker.Release pendingRelease;
    private String updateMessage = "Henüz kontrol edilmedi";
    private int updateMessageColor = 0;
    private boolean updateAuto = true;

    private final String[] pingTargets = {"1.1.1.1", "8.8.8.8", "9.9.9.9"};
    private final String[] pingLabels = {"CLOUDFLARE", "GOOGLE", "QUAD9"};

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (currentPage == 0) {
                refreshPanel();
                maybeCheckServers();
            } else if (currentPage == 1) {
                refreshPc(false);
            }
            handler.postDelayed(this, Math.max(1, refreshSeconds) * 1000L);
        }
    };

    private final Runnable updatePoll = new Runnable() {
        @Override
        public void run() {
            if (!updateDownloading || updateDownloadId < 0) {
                return;
            }
            UpdateChecker.Progress progress =
                    UpdateChecker.progress(MainActivity.this, updateDownloadId);
            if (progress.status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                finishUpdateDownload(true);
                return;
            }
            if (progress.status == android.app.DownloadManager.STATUS_FAILED) {
                finishUpdateDownload(false);
                return;
            }
            if (progress.percent >= 0) {
                setUpdateProgress(progress.percent);
                setUpdateStatus("Güncelleme indiriliyor: %" + progress.percent, ORANGE);
            } else {
                setUpdateStatus("Güncelleme indiriliyor…", ORANGE);
            }
            handler.postDelayed(this, 900L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPalette();
        prefs = getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE);
        loadAlertSettings();
        loadBehaviorSettings();
        loadPcSettings();
        loadUpdateSettings();
        noteStore = new NoteStore(this);
        reminderStore = new ReminderStore(this);
        endpointStore = new EndpointStore(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        NotificationHelper.ensureChannels(this);

        setContentView(buildRoot());
        registerPredictiveBack();
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
        showPage(pageFromIntent(getIntent()));
        refreshPanel();
        refreshNotes();
        refreshReminders();
        refreshEndpointManagement();
        refreshServerPanel();
        updateNotificationState();
        updateUpdateUi();
        maybeAutoCheckUpdate();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showPage(pageFromIntent(intent));
    }

    private int pageFromIntent(Intent intent) {
        if (intent == null) {
            return 0;
        }
        Object raw = intent.getExtras() == null ? null : intent.getExtras().get("page");
        int page = 0;
        if (raw instanceof Number) {
            page = ((Number) raw).intValue();
        } else if (raw != null) {
            try {
                page = Integer.parseInt(raw.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return Math.max(0, Math.min(3, page));
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        if (currentPage == 0) {
            refreshPanel();
            maybeCheckServers();
        } else if (currentPage == 1) {
            refreshPc(false);
        } else if (currentPage == 2) {
            restartSensorsIfNeeded();
        }
        if (updateDownloading) {
            handler.removeCallbacks(updatePoll);
            handler.post(updatePoll);
        }
        updateUpdateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        handler.removeCallbacks(updatePoll);
        stopSensors();
    }

    private void registerPredictiveBack() {
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
    }

    private void handleBack() {
        if (currentPage != 0) {
            showPage(0);
        } else {
            finish();
        }
    }

    @SuppressLint("GestureBackNavigation")
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
        updateMessageColor = MUTED;
    }

    private void loadAlertSettings() {
        alertsEnabled = prefs.getBoolean(AppPrefs.K_ALERTS, true);
        alertLow = prefs.getInt(AppPrefs.K_LOW, AppPrefs.DEFAULT_LOW);
        alertHigh = prefs.getInt(AppPrefs.K_HIGH, AppPrefs.DEFAULT_HIGH);
        alertTemp = prefs.getInt(AppPrefs.K_TEMP, AppPrefs.DEFAULT_TEMP);
    }

    private void loadBehaviorSettings() {
        quietEnabled = prefs.getBoolean(AppPrefs.K_QUIET, false);
        quietStart = prefs.getInt(AppPrefs.K_QUIET_START, AppPrefs.DEFAULT_QUIET_START);
        quietEnd = prefs.getInt(AppPrefs.K_QUIET_END, AppPrefs.DEFAULT_QUIET_END);
        int saved = prefs.getInt(AppPrefs.K_REFRESH, AppPrefs.DEFAULT_REFRESH);
        refreshSeconds = saved == 5 || saved == 10 ? saved : AppPrefs.DEFAULT_REFRESH;
    }

    private void loadPcSettings() {
        pcHost = prefs.getString(AppPrefs.K_PC_HOST, "");
        pcPort = prefs.getInt(AppPrefs.K_PC_PORT, AppPrefs.DEFAULT_PC_PORT);
        pcPin = prefs.getString(AppPrefs.K_PC_PIN, "");
        pcAuto = prefs.getBoolean(AppPrefs.K_PC_AUTO, true);
        if (pcHostInput != null) {
            pcHostInput.setText(pcHost);
            pcPortInput.setText(String.valueOf(pcPort));
            pcPinInput.setText(pcPin);
            pcAutoSwitch.setChecked(pcAuto);
        }
    }

    private void loadUpdateSettings() {
        updateAuto = prefs.getBoolean(AppPrefs.K_UPDATE_AUTO, true);
        if (updateAutoSwitch != null) {
            updateAutoSwitch.setChecked(updateAuto);
        }
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
        pcScroll = buildPcPage();
        toolsScroll = buildToolsPage();
        settingsScroll = buildSettingsPage();
        pageHost.addView(panelScroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        pageHost.addView(pcScroll, new FrameLayout.LayoutParams(MATCH, MATCH));
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

        String[] labels = {"PANEL", "PC", "ARAÇLAR", "AYARLAR"};
        int[] icons = {R.drawable.ic_nav_panel, R.drawable.ic_nav_pc,
                R.drawable.ic_nav_tools, R.drawable.ic_nav_info};
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
        pcScroll.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        toolsScroll.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        settingsScroll.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < navItems.length; i++) {
            boolean selected = i == index;
            navIcons[i].setColorFilter(selected ? ACCENT : MUTED);
            navLabels[i].setTextColor(selected ? ACCENT : MUTED);
            navItems[i].setBackground(selected ? bg(Color.parseColor("#141F33"), 14, 0) : null);
        }

        if (index == 0) {
            refreshPanel();
            maybeCheckServers();
        } else if (index == 1) {
            if (pcHost.isEmpty() || pcPin.isEmpty()) {
                setPcStatus("Windows YEK Panel'de PC Bağlantısı'nı açıp IP ve PIN girin", MUTED);
            } else if (!pcConnected) {
                if (pcAuto) {
                    connectPc(false);
                } else {
                    setPcStatus("Bağlanmak için BAĞLAN düğmesine dokunun", MUTED);
                }
            } else {
                refreshPc(false);
            }
        } else if (index == 2) {
            refreshToolsNetwork();
            refreshNotes();
            refreshReminders();
            refreshEndpointManagement();
            restartSensorsIfNeeded();
        } else {
            updateNotificationState();
            updateUpdateUi();
        }
        if (index != 2) {
            stopSensors();
        }
    }

    private void refreshAll() {
        refreshPanel();
        if (currentPage == 1) {
            refreshPc(true);
        }
        if (currentPage == 2) {
            refreshToolsNetwork();
            refreshNotes();
            refreshReminders();
            refreshEndpointManagement();
        }
        if (currentPage == 3) {
            updateNotificationState();
            updateUpdateUi();
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

        LinearLayout batteryCard = card();
        batteryCard.addView(cardHeader("PİL DETAYLARI", null));
        batteryVoltageValue = rowValue();
        addInfoRow(batteryCard, "Voltaj", batteryVoltageValue);
        batteryCurrentValue = rowValue();
        addInfoRow(batteryCard, "Akım", batteryCurrentValue);
        batteryChargeValue = rowValue();
        addInfoRow(batteryCard, "Şarj sayacı", batteryChargeValue);
        batteryTechValue = rowValue();
        addInfoRow(batteryCard, "Teknoloji", batteryTechValue);
        batterySourceValue = rowValue();
        addInfoRow(batteryCard, "Kaynak", batterySourceValue);
        addCard(c, batteryCard);

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

        LinearLayout netChips = new LinearLayout(this);
        netChips.setOrientation(LinearLayout.HORIZONTAL);
        netChips.setPadding(0, dp(9), 0, 0);
        netSignalChip = chip("— dBm", ACCENT);
        netLinkChip = chip("—", ACCENT);
        netChips.addView(netSignalChip);
        LinearLayout.LayoutParams linkLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        linkLp.leftMargin = dp(6);
        netChips.addView(netLinkChip, linkLp);
        netCard.addView(netChips);

        netGatewayValue = rowValue();
        addInfoRow(netCard, "Ağ geçidi", netGatewayValue);
        netDnsValue = rowValue();
        addInfoRow(netCard, "DNS", netDnsValue);
        externalIpValue = rowValue();
        externalIpValue.setText("Dokun → getir");
        externalIpValue.setTextColor(ACCENT);
        externalIpValue.setContentDescription("Dış IP adresini getir");
        externalIpValue.setOnClickListener(v -> {
            if (externalIpLoaded) {
                copyText("Dış IP", externalIpValue.getText().toString());
            } else {
                fetchExternalIp();
            }
        });
        addInfoRow(netCard, "Dış IP", externalIpValue);

        TextView netHint = text("Dış IP yalnızca dokununca sorgulanır; sonraki dokunuş kopyalar.",
                10, MUTED, false);
        netHint.setPadding(0, dp(8), 0, 0);
        netCard.addView(netHint);
        addCard(c, netCard);

        LinearLayout serverCard = card();
        serverSummary = chip("0 SUNUCU", MUTED);
        serverCard.addView(cardHeader("SUNUCULAR", serverSummary));
        serverList = new LinearLayout(this);
        serverList.setOrientation(LinearLayout.VERTICAL);
        serverCard.addView(serverList);
        serverCheckButton = button("TÜMÜNÜ KONTROL ET", false);
        LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        checkLp.topMargin = dp(10);
        serverCheckButton.setContentDescription("Tüm sunucuları kontrol et");
        serverCheckButton.setOnClickListener(v -> checkServers());
        serverCard.addView(serverCheckButton, checkLp);
        addCard(c, serverCard);

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
                + (alertsEnabled ? "uyarılar açık" : "uyarılar kapalı")
                + (quietEnabled && isQuietNow() ? " · sessiz saat" : ""));

        if (batteryVoltageValue != null) {
            batteryVoltageValue.setText(st.batteryVoltageMv > 0
                    ? String.format(Locale.getDefault(), "%.2f V",
                    st.batteryVoltageMv / 1000.0) : "—");
            long current = st.batteryCurrentUa;
            if (current == Long.MIN_VALUE || current == 0) {
                batteryCurrentValue.setText("—");
            } else {
                double ma = Math.abs(current) / 1000.0;
                batteryCurrentValue.setText(String.format(Locale.getDefault(),
                        "%.0f mA · %s", ma, st.charging ? "şarj" : "tüketim"));
            }
            batteryChargeValue.setText(st.batteryChargeCounterUah > 0
                    ? String.format(Locale.getDefault(), "%.0f mAh",
                    st.batteryChargeCounterUah / 1000.0) : "—");
            batteryTechValue.setText(st.batteryTechnology);
            batterySourceValue.setText(st.powerSource);
        }

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
        netIp.setText(st.localIp + (st.wifi ? " · Wi-Fi" : ""));
        applyChip(netChip, st.connected ? "BAĞLI" : "ÇEVRİMDIŞI", st.connected ? GREEN : RED);
        applyChip(netSignalChip, DeviceStats.signal(st.signalDbm),
                st.signalDbm > -120 && st.signalDbm < 0
                        ? (st.signalDbm > -70 ? GREEN : ORANGE) : MUTED);
        applyChip(netLinkChip, st.linkDownKbps > 0
                ? "↓ " + DeviceStats.speed(st.linkDownKbps) : "—", ACCENT);
        netGatewayValue.setText(st.gateway);
        netDnsValue.setText(st.dns);

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
        if (quietEnabled && isQuietNow()) {
            return;
        }
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

    // ---------------------------------------------------------------- PC

    private ScrollView buildPcPage() {
        ScrollView scroll = pageScroll();
        LinearLayout c = content(scroll);

        pcStateChip = chip(pcConnected ? "BAĞLI" : "BAĞLI DEĞİL", pcConnected ? GREEN : MUTED);
        c.addView(sectionHeader("PC BAĞLANTISI", pcStateChip));

        LinearLayout connectCard = card();
        connectCard.addView(cardHeader("YEK PANEL (WINDOWS)", chip("LAN · PIN", ACCENT)));
        TextView connectDesc = text("Windows'taki YEK Panel uygulamasında Ayarlar > "
                + "PC BAĞLANTISI bölümünü açın; ekranda görünen IP adresi ve 6 haneli PIN "
                + "ile bağlanın. Bağlantı yalnızca yerel ağda çalışır.", 11, MUTED, false);
        connectDesc.setPadding(0, dp(8), 0, dp(4));
        connectDesc.setLineSpacing(dp(3), 1f);
        connectCard.addView(connectDesc);

        connectCard.addView(pcInputLabel("BİLGİSAYAR IP ADRESİ"));
        pcHostInput = input("192.168.1.20", false);
        pcHostInput.setText(pcHost);
        pcHostInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI);
        connectCard.addView(pcHostInput);

        LinearLayout portPin = new LinearLayout(this);
        portPin.setOrientation(LinearLayout.HORIZONTAL);
        portPin.setPadding(0, dp(9), 0, 0);
        LinearLayout portBox = new LinearLayout(this);
        portBox.setOrientation(LinearLayout.VERTICAL);
        portBox.addView(pcInputLabel("PORT"));
        pcPortInput = input("8787", false);
        pcPortInput.setText(String.valueOf(pcPort));
        pcPortInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portBox.addView(pcPortInput);
        portPin.addView(portBox, new LinearLayout.LayoutParams(0, WRAP, 0.42f));

        LinearLayout pinBox = new LinearLayout(this);
        pinBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams pinBoxLp = new LinearLayout.LayoutParams(0, WRAP, 0.58f);
        pinBoxLp.leftMargin = dp(8);
        pinBox.addView(pcInputLabel("6 HANELİ PIN"));
        pcPinInput = input("6 hane", false);
        pcPinInput.setText(pcPin);
        pcPinInput.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinBox.addView(pcPinInput);
        portPin.addView(pinBox, pinBoxLp);
        connectCard.addView(portPin);

        LinearLayout connectRow = new LinearLayout(this);
        connectRow.setOrientation(LinearLayout.HORIZONTAL);
        connectRow.setPadding(0, dp(10), 0, 0);
        pcConnectButton = button("BAĞLAN", true);
        pcConnectButton.setContentDescription("Bilgisayara bağlan");
        pcConnectButton.setOnClickListener(v -> connectPc(true));
        pcDiscoverButton = button("PC'Yİ BUL", false);
        pcDiscoverButton.setContentDescription("Yerel ağda bilgisayar ara");
        pcDiscoverButton.setOnClickListener(v -> discoverPc());
        connectRow.addView(pcConnectButton, new LinearLayout.LayoutParams(0, WRAP, 1.3f));
        LinearLayout.LayoutParams discoverLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        discoverLp.leftMargin = dp(6);
        connectRow.addView(pcDiscoverButton, discoverLp);
        connectCard.addView(connectRow);

        LinearLayout autoRow = new LinearLayout(this);
        autoRow.setOrientation(LinearLayout.HORIZONTAL);
        autoRow.setGravity(Gravity.CENTER_VERTICAL);
        autoRow.setPadding(0, dp(11), 0, 0);
        TextView autoLabel = text("PC sekmesinde otomatik bağlan", 12, TEXT, false);
        autoRow.addView(autoLabel, new LinearLayout.LayoutParams(0, WRAP, 1f));
        pcAutoSwitch = new Switch(this);
        pcAutoSwitch.setShowText(false);
        pcAutoSwitch.setChecked(pcAuto);
        pcAutoSwitch.setThumbTintList(ColorStateList.valueOf(ACCENT));
        pcAutoSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#1E2A44")));
        pcAutoSwitch.setContentDescription("PC sekmesinde otomatik bağlan");
        pcAutoSwitch.setOnCheckedChangeListener((button, checked) -> {
            pcAuto = checked;
            prefs.edit().putBoolean(AppPrefs.K_PC_AUTO, checked).apply();
        });
        autoRow.addView(pcAutoSwitch);
        connectCard.addView(autoRow);

        pcConnectState = text("", 10, MUTED, false);
        pcConnectState.setPadding(0, dp(9), 0, 0);
        pcConnectState.setLineSpacing(dp(2), 1f);
        connectCard.addView(pcConnectState);
        setPcStatus(pcConnected ? "Bağlı" : "Windows YEK Panel'de PC Bağlantısı'nı açıp IP ve PIN girin",
                pcConnected ? GREEN : MUTED);
        addCard(c, connectCard);

        LinearLayout liveCard = card();
        liveCard.addView(cardHeader("CANLI DURUM", chip("YEREL AĞ", ACCENT)));
        pcName = valueText("—", 22, TEXT, true);
        pcName.setPadding(0, dp(8), 0, 0);
        liveCard.addView(pcName);
        pcMeta = text("Bağlantı bekleniyor", 11, MUTED, false);
        pcMeta.setPadding(0, dp(3), 0, dp(2));
        liveCard.addView(pcMeta);

        pcIpValue = rowValue();
        addInfoRow(liveCard, "IP adresleri", pcIpValue);
        pcIpValue.setText("—");

        pcCpuMeter = new MeterView(this);
        pcCpuValue = valueText("—", 15, TEXT, true);
        liveCard.addView(pcMetricBlock("CPU", pcCpuValue, pcCpuMeter));

        pcRamMeter = new MeterView(this);
        pcRamValue = valueText("—", 12, TEXT, false);
        liveCard.addView(pcMetricBlock("RAM", pcRamValue, pcRamMeter));

        pcNetValue = rowValue();
        addInfoRow(liveCard, "Ağ trafiği", pcNetValue);
        pcNetValue.setText("—");
        pcLinkValue = rowValue();
        addInfoRow(liveCard, "Bağlantı hızı", pcLinkValue);
        pcLinkValue.setText("—");
        pcTempValue = rowValue();
        addInfoRow(liveCard, "Sıcaklık", pcTempValue);
        pcTempValue.setText("—");
        pcBatteryValue = rowValue();
        addInfoRow(liveCard, "Pil", pcBatteryValue);
        pcBatteryValue.setText("—");

        TextView diskTitle = labelText("DİSKLER");
        diskTitle.setPadding(0, dp(12), 0, dp(2));
        liveCard.addView(diskTitle);
        pcDiskList = new LinearLayout(this);
        pcDiskList.setOrientation(LinearLayout.VERTICAL);
        liveCard.addView(pcDiskList);
        TextView noDisk = text("Bağlanınca disk kullanımı görünür.", 10, MUTED, false);
        pcDiskList.addView(noDisk);

        pcUpdatedValue = text("Henüz veri alınmadı", 10, MUTED, false);
        pcUpdatedValue.setPadding(0, dp(10), 0, 0);
        liveCard.addView(pcUpdatedValue);
        addCard(c, liveCard);

        LinearLayout processCard = card();
        processCard.addView(cardHeader("EN YOĞUN İŞLEMLER", null));
        pcProcessList = new LinearLayout(this);
        pcProcessList.setOrientation(LinearLayout.VERTICAL);
        TextView noProcess = text("Bağlanınca belleği en çok kullanan 5 işlem listelenir.",
                10, MUTED, false);
        noProcess.setPadding(0, dp(8), 0, 0);
        pcProcessList.addView(noProcess);
        processCard.addView(pcProcessList);
        addCard(c, processCard);

        LinearLayout helpCard = card();
        helpCard.addView(cardHeader("NASIL ÇALIŞIR?", null));
        String[] steps = {
                "1. Windows'ta YEK Panel uygulamasını açın.",
                "2. Ayarlar > PC BAĞLANTISI bölümünde bağlantıyı açın; varsayılan port 8787'dir.",
                "3. Telefon ile bilgisayar aynı Wi-Fi ağında olmalıdır.",
                "4. Ekranda görünen IP adresini ve 6 haneli PIN'i bu sayfaya girin; "
                        + "PC'Yİ BUL ile otomatik arama yapabilirsiniz.",
                "5. Veriler internete gönderilmez; 5 hatalı PIN denemesinde bağlantı 1 dakika kilitlenir."
        };
        for (String step : steps) {
            TextView item = text(step, 11, MUTED, false);
            item.setPadding(0, dp(7), 0, 0);
            item.setLineSpacing(dp(3), 1f);
            helpCard.addView(item);
        }
        addCard(c, helpCard);

        return scroll;
    }

    private TextView pcInputLabel(String value) {
        TextView label = labelText(value);
        label.setPadding(0, dp(10), 0, dp(5));
        return label;
    }

    private View pcMetricBlock(String label, TextView value, MeterView meter) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(11), 0, 0);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(labelText(label), new LinearLayout.LayoutParams(0, WRAP, 1f));
        head.addView(value);
        block.addView(head);
        LinearLayout.LayoutParams meterLp = new LinearLayout.LayoutParams(MATCH, dp(6));
        meterLp.topMargin = dp(6);
        block.addView(meter, meterLp);
        return block;
    }

    private void readPcInputs() {
        if (pcHostInput != null) {
            pcHost = PcClient.normalizeHost(pcHostInput.getText().toString());
        }
        if (pcPortInput != null) {
            try {
                pcPort = Integer.parseInt(pcPortInput.getText().toString().trim());
            } catch (NumberFormatException ignored) {
                pcPort = AppPrefs.DEFAULT_PC_PORT;
            }
        }
        if (pcPort < 1 || pcPort > 65535) {
            pcPort = AppPrefs.DEFAULT_PC_PORT;
        }
        if (pcPinInput != null) {
            pcPin = pcPinInput.getText().toString().trim();
        }
    }

    private void savePcConfig() {
        prefs.edit()
                .putString(AppPrefs.K_PC_HOST, pcHost)
                .putInt(AppPrefs.K_PC_PORT, pcPort)
                .putString(AppPrefs.K_PC_PIN, pcPin)
                .putBoolean(AppPrefs.K_PC_AUTO, pcAuto)
                .apply();
    }

    private boolean hasLocalNetworkPermission() {
        if (Build.VERSION.SDK_INT < 37) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("InlinedApi")
    private boolean ensureLocalNetworkPermission(int action) {
        if (hasLocalNetworkPermission()) {
            return true;
        }
        pcPendingAction = action;
        setPcState("İZİN GEREKLİ", ORANGE);
        setPcStatus("Yerel ağ izni gerekli: aynı ağdaki PC'ye bağlanmak için izin verin", ORANGE);
        requestPermissions(new String[]{Manifest.permission.ACCESS_LOCAL_NETWORK},
                REQ_LOCAL_NETWORK);
        return false;
    }

    private void connectPc(boolean userAction) {
        if (pcConnecting || pcFetching) {
            return;
        }
        readPcInputs();
        savePcConfig();
        if (pcHost.isEmpty()) {
            setPcState("BAĞLI DEĞİL", ORANGE);
            setPcStatus("Bilgisayar IP adresi girin", ORANGE);
            if (userAction) {
                Toast.makeText(this, "Bilgisayar IP adresi gerekli", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (!pcPin.matches("\\d{6}")) {
            setPcState("PIN GEREKLİ", ORANGE);
            setPcStatus("PC Bağlantısı ekranındaki 6 haneli PIN'i girin", ORANGE);
            if (userAction) {
                Toast.makeText(this, "6 haneli PIN girin", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (userAction && !ensureLocalNetworkPermission(1)) {
            return;
        }
        if (!hasLocalNetworkPermission()) {
            setPcState("BAĞLI DEĞİL", ORANGE);
            setPcStatus("Yerel ağ izni gerekli", ORANGE);
            return;
        }
        final String host = pcHost;
        final int port = pcPort;
        final String pin = pcPin;
        pcConnecting = true;
        pcFetching = true;
        setPcState("BAĞLANIYOR", ORANGE);
        setPcStatus("Bağlantı kuruluyor: " + host + ":" + port, MUTED);
        if (pcConnectButton != null) {
            pcConnectButton.setEnabled(false);
        }
        new Thread(() -> {
            PcClient.Result result = PcClient.fetch(host, port, pin);
            handler.post(() -> {
                pcConnecting = false;
                pcFetching = false;
                if (pcConnectButton != null) {
                    pcConnectButton.setEnabled(true);
                }
                if (result.status != null) {
                    pcConnected = true;
                    pcStatus = result.status;
                    pcNextRetryAt = 0;
                    applyPcStatus();
                    setPcState("BAĞLI", GREEN);
                    setPcStatus("Bağlı · " + result.status.name + " · " + host + ":" + port,
                            GREEN);
                    if (userAction) {
                        Toast.makeText(this, "PC bağlantısı kuruldu", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    pcConnected = false;
                    pcNextRetryAt = result.unauthorized
                            ? System.currentTimeMillis() + 60_000L
                            : System.currentTimeMillis() + 5_000L;
                    setPcState(result.unauthorized ? "PIN HATALI" : "BAĞLI DEĞİL",
                            result.unauthorized ? ORANGE : RED);
                    setPcStatus(result.error, result.unauthorized ? ORANGE : RED);
                    if (userAction) {
                        Toast.makeText(MainActivity.this, result.error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }, "yek-pc-connect").start();
    }

    private void refreshPc(boolean userAction) {
        if (pcConnecting || pcFetching) {
            return;
        }
        readPcInputs();
        if (pcHost.isEmpty() || !pcPin.matches("\\d{6}")) {
            if (userAction) {
                connectPc(true);
            }
            return;
        }
        if (!pcConnected) {
            if (!userAction && !pcAuto) {
                return;
            }
            if (!userAction && System.currentTimeMillis() < pcNextRetryAt) {
                return;
            }
            connectPc(userAction);
            return;
        }
        final String host = pcHost;
        final int port = pcPort;
        final String pin = pcPin;
        pcFetching = true;
        new Thread(() -> {
            PcClient.Result result = PcClient.fetch(host, port, pin);
            handler.post(() -> {
                pcFetching = false;
                if (result.status != null) {
                    pcConnected = true;
                    pcStatus = result.status;
                    pcNextRetryAt = 0;
                    applyPcStatus();
                    setPcState("BAĞLI", GREEN);
                    setPcStatus("Bağlı · " + result.status.name + " · " + host + ":" + port,
                            GREEN);
                } else {
                    pcConnected = false;
                    pcNextRetryAt = result.unauthorized
                            ? System.currentTimeMillis() + 60_000L
                            : System.currentTimeMillis() + 5_000L;
                    setPcState(result.unauthorized ? "PIN HATALI" : "BAĞLANTI YOK",
                            result.unauthorized ? ORANGE : RED);
                    setPcStatus(result.error, result.unauthorized ? ORANGE : RED);
                    if (userAction) {
                        Toast.makeText(MainActivity.this, result.error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }, "yek-pc-refresh").start();
    }

    private void discoverPc() {
        if (pcDiscovering) {
            return;
        }
        readPcInputs();
        if (!ensureLocalNetworkPermission(2)) {
            return;
        }
        pcDiscovering = true;
        setPcState("ARANIYOR", ORANGE);
        setPcStatus("Yerel ağda YEK Panel aranıyor…", MUTED);
        if (pcDiscoverButton != null) {
            pcDiscoverButton.setEnabled(false);
        }
        new Thread(() -> {
            PcClient.Found found = PcClient.discover(2600);
            handler.post(() -> {
                pcDiscovering = false;
                if (pcDiscoverButton != null) {
                    pcDiscoverButton.setEnabled(true);
                }
                if (found == null) {
                    setPcState("BAĞLI DEĞİL", MUTED);
                    setPcStatus("PC bulunamadı. IP adresini elle girin; Windows güvenlik "
                            + "duvarı yerel ağ iznini engelliyor olabilir.", ORANGE);
                    Toast.makeText(MainActivity.this, "PC bulunamadı",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                pcHost = found.host;
                pcPort = found.port;
                if (pcHostInput != null) {
                    pcHostInput.setText(pcHost);
                    pcPortInput.setText(String.valueOf(pcPort));
                }
                savePcConfig();
                setPcState("BULUNDU", GREEN);
                setPcStatus("Bulundu: " + found.name + " · " + pcHost + ":" + pcPort
                        + " · PIN'i girin", GREEN);
                Toast.makeText(MainActivity.this,
                        "PC bulundu: " + found.name, Toast.LENGTH_SHORT).show();
                if (pcPinInput != null) {
                    pcPinInput.requestFocus();
                }
            });
        }, "yek-pc-discover").start();
    }

    private void applyPcStatus() {
        PcClient.Status status = pcStatus;
        if (status == null) {
            return;
        }
        if (pcName != null) {
            pcName.setText(status.name == null || status.name.isEmpty() ? "PC" : status.name);
        }
        if (pcMeta != null) {
            String os = status.os == null || status.os.isEmpty() ? "Windows" : status.os;
            pcMeta.setText(os + " · " + PcClient.uptime(status.uptimeSec));
        }
        if (pcIpValue != null) {
            pcIpValue.setText(status.ips.isEmpty() ? "—" : joinText(status.ips));
        }
        if (pcCpuValue != null) {
            pcCpuValue.setText(String.format(Locale.getDefault(), "%.1f %%", status.cpuPercent));
        }
        if (pcCpuMeter != null) {
            pcCpuMeter.setProgress((int) Math.round(status.cpuPercent),
                    loadColor(status.cpuPercent));
        }
        if (pcRamValue != null) {
            pcRamValue.setText(PcClient.bytes(status.ramUsed) + " / "
                    + PcClient.bytes(status.ramTotal) + " · %" + status.ramPercent);
        }
        if (pcRamMeter != null) {
            pcRamMeter.setProgress(status.ramPercent, loadColor(status.ramPercent));
        }
        if (pcNetValue != null) {
            pcNetValue.setText("↓ " + PcClient.rate(status.netDown)
                    + " · ↑ " + PcClient.rate(status.netUp));
        }
        if (pcLinkValue != null) {
            pcLinkValue.setText(status.linkMbps > 0
                    ? status.linkMbps + " Mbps" : "—");
        }
        if (pcTempValue != null) {
            pcTempValue.setText(Double.isNaN(status.tempC) ? "—"
                    : String.format(Locale.getDefault(), "%.1f °C", status.tempC));
        }
        if (pcBatteryValue != null) {
            pcBatteryValue.setText(status.batteryPercent < 0 ? "Masaüstü (pil yok)"
                    : "%" + status.batteryPercent + (status.batteryCharging
                    ? " · şarj oluyor" : " · pil"));
        }
        if (pcUpdatedValue != null) {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date(status.receivedAt));
            pcUpdatedValue.setText("Son güncelleme: " + time
                    + " · köprü v" + status.version);
        }
        if (pcDiskList != null) {
            pcDiskList.removeAllViews();
            if (status.disks.isEmpty()) {
                TextView empty = text("Disk bilgisi yok.", 10, MUTED, false);
                empty.setPadding(0, dp(6), 0, 0);
                pcDiskList.addView(empty);
            } else {
                for (PcClient.Disk disk : status.disks) {
                    pcDiskList.addView(pcDiskRow(disk));
                }
            }
        }
        if (pcProcessList != null) {
            pcProcessList.removeAllViews();
            if (status.processes.isEmpty()) {
                TextView empty = text("İşlem bilgisi yok.", 10, MUTED, false);
                empty.setPadding(0, dp(6), 0, 0);
                pcProcessList.addView(empty);
            } else {
                for (PcClient.ProcessInfo process : status.processes) {
                    pcProcessList.addView(pcProcessRow(process));
                }
            }
        }
    }

    private View pcDiskRow(PcClient.Disk disk) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(7), 0, 0);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(disk.name, 12, TEXT, true);
        head.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));
        long used = Math.max(0, disk.total - disk.free);
        TextView value = text(PcClient.bytes(used) + " / " + PcClient.bytes(disk.total)
                + " · %" + disk.percent, 10, MUTED, false);
        head.addView(value);
        block.addView(head);
        MeterView meter = new MeterView(this);
        meter.setProgress(disk.percent, loadColor(disk.percent));
        LinearLayout.LayoutParams meterLp = new LinearLayout.LayoutParams(MATCH, dp(5));
        meterLp.topMargin = dp(5);
        block.addView(meter, meterLp);
        return block;
    }

    private View pcProcessRow(PcClient.ProcessInfo process) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView name = text(process.name, 12, TEXT, false);
        name.setMaxLines(1);
        row.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView meta = text(String.format(Locale.getDefault(), "%.0f MB · PID %d",
                process.memMb, process.pid), 10, MUTED, false);
        row.addView(meta);
        return row;
    }

    private int loadColor(double percent) {
        if (percent >= 85) {
            return RED;
        }
        if (percent >= 60) {
            return ORANGE;
        }
        return GREEN;
    }

    private String joinText(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private void setPcState(String label, int color) {
        if (pcStateChip != null) {
            applyChip(pcStateChip, label, color);
        }
    }

    private void setPcStatus(String message, int color) {
        if (pcConnectState != null) {
            pcConnectState.setText(message);
            pcConnectState.setTextColor(color);
        }
    }

    private void resetPcUi() {
        setPcState("BAĞLI DEĞİL", MUTED);
        setPcStatus("Windows YEK Panel'de PC Bağlantısı'nı açıp IP ve PIN girin", MUTED);
        if (pcName != null) {
            pcName.setText("—");
        }
        if (pcMeta != null) {
            pcMeta.setText("Bağlantı bekleniyor");
        }
        if (pcIpValue != null) {
            pcIpValue.setText("—");
        }
        if (pcCpuValue != null) {
            pcCpuValue.setText("—");
        }
        if (pcCpuMeter != null) {
            pcCpuMeter.setProgress(0, MUTED);
        }
        if (pcRamValue != null) {
            pcRamValue.setText("—");
        }
        if (pcRamMeter != null) {
            pcRamMeter.setProgress(0, MUTED);
        }
        if (pcNetValue != null) {
            pcNetValue.setText("—");
        }
        if (pcLinkValue != null) {
            pcLinkValue.setText("—");
        }
        if (pcTempValue != null) {
            pcTempValue.setText("—");
        }
        if (pcBatteryValue != null) {
            pcBatteryValue.setText("—");
        }
        if (pcUpdatedValue != null) {
            pcUpdatedValue.setText("Henüz veri alınmadı");
        }
        if (pcDiskList != null) {
            pcDiskList.removeAllViews();
            TextView empty = text("Bağlanınca disk kullanımı görünür.", 10, MUTED, false);
            pcDiskList.addView(empty);
        }
        if (pcProcessList != null) {
            pcProcessList.removeAllViews();
            TextView empty = text("Bağlanınca belleği en çok kullanan 5 işlem listelenir.",
                    10, MUTED, false);
            empty.setPadding(0, dp(8), 0, 0);
            pcProcessList.addView(empty);
        }
    }

    // ---------------------------------------------------------------- Araçlar

    private ScrollView buildToolsPage() {
        ScrollView scroll = pageScroll();
        LinearLayout c = content(scroll);

        c.addView(sectionHeader("ARAÇLAR", chip("7 ARAÇ", ACCENT)));

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

        LinearLayout speedCard = card();
        speedCard.addView(cardHeader("HIZ TESTİ", chip("8 MB", ACCENT)));
        TextView speedDesc = text("İndirme hızını OVH, Cloudflare ve Hetzner sırasıyla ölçer. "
                + "Test yaklaşık 8 MB veri kullanır.", 11, MUTED, false);
        speedDesc.setPadding(0, dp(6), 0, 0);
        speedDesc.setLineSpacing(dp(3), 1f);
        speedCard.addView(speedDesc);

        speedButton = button("İNDİRME HIZINI ÖLÇ", true);
        LinearLayout.LayoutParams speedLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        speedLp.topMargin = dp(12);
        speedButton.setContentDescription("İndirme hızını ölç");
        speedButton.setOnClickListener(v -> runSpeedTest());
        speedCard.addView(speedButton, speedLp);

        speedProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        speedProgress.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
        speedProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams speedProgressLp =
                new LinearLayout.LayoutParams(dp(22), dp(22));
        speedProgressLp.gravity = Gravity.CENTER_HORIZONTAL;
        speedProgressLp.topMargin = dp(8);
        speedCard.addView(speedProgress, speedProgressLp);

        speedResult = text("Hazır. Test başlatıldığında sonuç burada görünür.", 12, MUTED, false);
        speedResult.setPadding(0, dp(8), 0, 0);
        speedResult.setLineSpacing(dp(3), 1f);
        speedCard.addView(speedResult);
        addCard(c, speedCard);

        LinearLayout diagCard = card();
        diagCard.addView(cardHeader("AĞ TANILAMA", chip("DNS · TCP", ACCENT)));
        TextView diagDesc = text("Adres çözümleme süresini ve bir TCP portunun açık olup "
                + "olmadığını sınar.", 11, MUTED, false);
        diagDesc.setPadding(0, dp(6), 0, 0);
        diagDesc.setLineSpacing(dp(3), 1f);
        diagCard.addView(diagDesc);

        LinearLayout diagInputs = new LinearLayout(this);
        diagInputs.setOrientation(LinearLayout.HORIZONTAL);
        diagHostInput = input("Adres (örn. cloudflare.com)", false);
        diagHostInput.setText("cloudflare.com");
        diagHostInput.setContentDescription("Tanılama adresi");
        diagPortInput = input("Port", false);
        diagPortInput.setText("443");
        diagPortInput.setContentDescription("Port numarası");
        diagInputs.addView(diagHostInput, new LinearLayout.LayoutParams(0, WRAP, 2.2f));
        LinearLayout.LayoutParams portLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        portLp.leftMargin = dp(6);
        diagInputs.addView(diagPortInput, portLp);
        LinearLayout.LayoutParams diagInputsLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        diagInputsLp.topMargin = dp(10);
        diagCard.addView(diagInputs, diagInputsLp);

        LinearLayout diagActions = new LinearLayout(this);
        diagActions.setOrientation(LinearLayout.HORIZONTAL);
        TextView dnsButton = button("DNS SORGULA", true);
        dnsButton.setContentDescription("DNS sorgula");
        dnsButton.setOnClickListener(v -> runDnsLookup());
        TextView portButton = button("PORT TEST ET", false);
        portButton.setContentDescription("Port test et");
        portButton.setOnClickListener(v -> runPortTest());
        diagActions.addView(dnsButton, new LinearLayout.LayoutParams(0, WRAP, 1f));
        LinearLayout.LayoutParams portButtonLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        portButtonLp.leftMargin = dp(6);
        diagActions.addView(portButton, portButtonLp);
        LinearLayout.LayoutParams diagActionsLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        diagActionsLp.topMargin = dp(8);
        diagCard.addView(diagActions, diagActionsLp);

        diagResult = text("Hazır. Bir sorgu seçin.", 12, MUTED, false);
        diagResult.setPadding(0, dp(8), 0, 0);
        diagResult.setLineSpacing(dp(3), 1f);
        diagCard.addView(diagResult);
        addCard(c, diagCard);

        LinearLayout serversCard = card();
        endpointSummary = chip("0 / 8", ACCENT);
        serversCard.addView(cardHeader("SUNUCULAR", endpointSummary));
        TextView serversDesc = text("Web sitesi veya panel adresi ekleyin. Panel sekmesinde "
                + "durum, gecikme ve SSL kalan gün gösterilir.", 11, MUTED, false);
        serversDesc.setPadding(0, dp(6), 0, 0);
        serversDesc.setLineSpacing(dp(3), 1f);
        serversCard.addView(serversDesc);

        endpointNameInput = input("Sunucu adı (örn. Site)", false);
        endpointNameInput.setContentDescription("Sunucu adı");
        LinearLayout.LayoutParams endpointNameLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        endpointNameLp.topMargin = dp(10);
        serversCard.addView(endpointNameInput, endpointNameLp);

        endpointUrlInput = input("Adres (örn. https://ornek.com)", false);
        endpointUrlInput.setContentDescription("Sunucu adresi");
        LinearLayout.LayoutParams endpointUrlLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        endpointUrlLp.topMargin = dp(8);
        serversCard.addView(endpointUrlInput, endpointUrlLp);

        TextView addEndpoint = button("SUNUCU EKLE", true);
        LinearLayout.LayoutParams addEndpointLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        addEndpointLp.topMargin = dp(10);
        addEndpoint.setContentDescription("Sunucu ekle");
        addEndpoint.setOnClickListener(v -> addEndpoint());
        serversCard.addView(addEndpoint, addEndpointLp);

        endpointManageList = new LinearLayout(this);
        endpointManageList.setOrientation(LinearLayout.VERTICAL);
        endpointManageList.setPadding(0, dp(6), 0, 0);
        serversCard.addView(endpointManageList);
        addCard(c, serversCard);

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

        LinearLayout sensorsCard = card();
        sensorSwitch = new Switch(this);
        sensorSwitch.setShowText(false);
        sensorSwitch.setChecked(false);
        sensorSwitch.setThumbTintList(ColorStateList.valueOf(ACCENT));
        sensorSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#1E2A44")));
        sensorSwitch.setContentDescription("Canlı sensörler");
        sensorSwitch.setOnCheckedChangeListener((button, checked) ->
                setSensorsEnabled(checked, true));
        sensorsCard.addView(cardHeader("SENSÖRLER", sensorSwitch));
        TextView sensorsDesc = text("İvmeölçer, jiroskop, ışık, yakınlık, basınç ve adım "
                + "sayacı gibi sensörleri canlı okur. Pil tüketimini azaltmak için yalnızca "
                + "bu sekme açıkken çalışır.", 11, MUTED, false);
        sensorsDesc.setPadding(0, dp(6), 0, 0);
        sensorsDesc.setLineSpacing(dp(3), 1f);
        sensorsCard.addView(sensorsDesc);

        sensorList = new LinearLayout(this);
        sensorList.setOrientation(LinearLayout.VERTICAL);
        sensorList.setPadding(0, dp(6), 0, 0);
        sensorsCard.addView(sensorList);
        showSensorsOffMessage();
        addCard(c, sensorsCard);

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

    // ---------------------------------------------------------------- Sunucular

    private void addEndpoint() {
        if (endpointNameInput == null || endpointUrlInput == null) {
            return;
        }
        String name = endpointNameInput.getText().toString().trim();
        String rawUrl = endpointUrlInput.getText().toString().trim();
        if (rawUrl.isEmpty()) {
            Toast.makeText(this, "Sunucu adresi gerekli", Toast.LENGTH_SHORT).show();
            return;
        }
        if (EndpointStore.normalizeUrl(rawUrl) == null) {
            Toast.makeText(this, "Geçerli bir adres girin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endpointStore.all().size() >= EndpointStore.MAX_ENDPOINTS) {
            Toast.makeText(this, "En fazla " + EndpointStore.MAX_ENDPOINTS
                    + " sunucu eklenebilir", Toast.LENGTH_SHORT).show();
            return;
        }
        EndpointStore.Endpoint endpoint = endpointStore.add(name, rawUrl);
        if (endpoint == null) {
            Toast.makeText(this, "Bu adres zaten ekli", Toast.LENGTH_SHORT).show();
            return;
        }
        endpointNameInput.setText("");
        endpointUrlInput.setText("");
        refreshEndpointManagement();
        refreshServerPanel();
        Toast.makeText(this, endpoint.name + " eklendi", Toast.LENGTH_SHORT).show();
    }

    private void refreshEndpointManagement() {
        if (endpointManageList == null || endpointSummary == null) {
            return;
        }
        List<EndpointStore.Endpoint> endpoints = endpointStore.all();
        endpointSummary.setText(endpoints.size() + " / " + EndpointStore.MAX_ENDPOINTS);
        endpointManageList.removeAllViews();
        if (endpoints.isEmpty()) {
            TextView empty = text("Henüz sunucu yok. İlk adresi ekleyin.", 11, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(14), 0, dp(8));
            endpointManageList.addView(empty);
            return;
        }
        for (EndpointStore.Endpoint endpoint : endpoints) {
            endpointManageList.addView(endpointRow(endpoint));
        }
    }

    private View endpointRow(EndpointStore.Endpoint endpoint) {
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
        TextView name = text(endpoint.name, 13, TEXT, true);
        name.setMaxLines(1);
        head.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView delete = actionChip("SİL", RED);
        delete.setOnClickListener(v -> confirmDeleteEndpoint(endpoint));
        head.addView(delete);
        box.addView(head);

        TextView url = text(endpoint.url, 10, MUTED, false);
        url.setMaxLines(1);
        url.setEllipsize(android.text.TextUtils.TruncateAt.END);
        url.setPadding(0, dp(3), 0, 0);
        box.addView(url);

        EndpointChecker.Result result = serverResults.get(endpoint.id);
        String meta;
        int color;
        if (result == null) {
            meta = "Henüz kontrol edilmedi";
            color = MUTED;
        } else if (!result.error.isEmpty()) {
            meta = result.error;
            color = RED;
        } else {
            meta = EndpointChecker.latencyLabel(result)
                    + (result.sslDays >= 0 ? " · SSL " + result.sslDays + " gün" : "");
            color = result.healthy() ? GREEN : ORANGE;
        }
        TextView status = text(meta, 9, color, false);
        status.setPadding(0, dp(5), 0, 0);
        box.addView(status);
        return box;
    }

    private void confirmDeleteEndpoint(EndpointStore.Endpoint endpoint) {
        new AlertDialog.Builder(this)
                .setTitle("Sunucu silinsin mi?")
                .setMessage(endpoint.name + "\n" + endpoint.url)
                .setPositiveButton("Sil", (dialog, which) -> {
                    endpointStore.delete(endpoint.id);
                    serverResults.remove(endpoint.id);
                    refreshEndpointManagement();
                    refreshServerPanel();
                    Toast.makeText(this, "Sunucu silindi", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void refreshServerPanel() {
        if (serverList == null || serverSummary == null) {
            return;
        }
        List<EndpointStore.Endpoint> endpoints = endpointStore.all();
        int healthy = 0;
        for (EndpointStore.Endpoint endpoint : endpoints) {
            EndpointChecker.Result result = serverResults.get(endpoint.id);
            if (result != null && result.healthy()) {
                healthy++;
            }
        }
        if (endpoints.isEmpty()) {
            applyChip(serverSummary, "0 SUNUCU", MUTED);
        } else {
            int color = healthy == endpoints.size() ? GREEN : (healthy == 0 ? RED : ORANGE);
            applyChip(serverSummary, healthy + "/" + endpoints.size() + " ÇEVRİMİÇİ", color);
        }
        serverList.removeAllViews();
        if (endpoints.isEmpty()) {
            TextView empty = text("Henüz sunucu eklenmedi. Araçlar → Sunucular'dan ekleyin.",
                    11, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(14), 0, dp(6));
            serverList.addView(empty);
        } else {
            for (EndpointStore.Endpoint endpoint : endpoints) {
                serverList.addView(serverRow(endpoint));
            }
        }
        if (serverCheckButton != null) {
            serverCheckButton.setEnabled(!checkingServers);
            serverCheckButton.setAlpha(checkingServers ? 0.55f : 1f);
            serverCheckButton.setText(checkingServers ? "KONTROL EDİLİYOR…"
                    : "TÜMÜNÜ KONTROL ET");
        }
    }

    private View serverRow(EndpointStore.Endpoint endpoint) {
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
        TextView name = text(endpoint.name, 13, TEXT, true);
        name.setMaxLines(1);
        head.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));
        EndpointChecker.Result result = serverResults.get(endpoint.id);
        int color = result == null ? MUTED
                : (result.healthy() ? GREEN : (result.reachable() ? ORANGE : RED));
        head.addView(chip(EndpointChecker.codeLabel(result), color));
        box.addView(head);

        TextView url = text(endpoint.url, 9, MUTED, false);
        url.setMaxLines(1);
        url.setEllipsize(android.text.TextUtils.TruncateAt.END);
        url.setPadding(0, dp(3), 0, 0);
        box.addView(url);

        String meta;
        if (result == null) {
            meta = "Henüz kontrol edilmedi";
        } else if (!result.error.isEmpty()) {
            meta = result.error;
        } else {
            meta = EndpointChecker.latencyLabel(result)
                    + (result.sslDays >= 0 ? " · SSL " + result.sslDays + " gün" : "");
        }
        TextView detail = text(meta, 9,
                result != null && result.sslDays >= 0 && result.sslDays < 15 ? ORANGE : MUTED,
                false);
        detail.setPadding(0, dp(5), 0, 0);
        box.addView(detail);
        return box;
    }

    private void maybeCheckServers() {
        if (checkingServers || endpointStore == null || currentPage != 0) {
            return;
        }
        if (endpointStore.all().isEmpty()) {
            return;
        }
        if (System.currentTimeMillis() - lastServerCheckAt < SERVER_CHECK_INTERVAL) {
            return;
        }
        checkServers();
    }

    private void checkServers() {
        if (checkingServers) {
            return;
        }
        final List<EndpointStore.Endpoint> endpoints = endpointStore.all();
        if (endpoints.isEmpty()) {
            Toast.makeText(this, "Önce Araçlar → Sunucular'dan adres ekleyin",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        checkingServers = true;
        lastServerCheckAt = System.currentTimeMillis();
        refreshServerPanel();
        new Thread(() -> {
            final Map<Long, EndpointChecker.Result> checked = new HashMap<>();
            for (EndpointStore.Endpoint endpoint : endpoints) {
                checked.put(endpoint.id, EndpointChecker.check(endpoint.url));
            }
            runOnUiThread(() -> {
                checkingServers = false;
                lastServerCheckAt = System.currentTimeMillis();
                serverResults.clear();
                serverResults.putAll(checked);
                refreshServerPanel();
                refreshEndpointManagement();
            });
        }, "yek-servers").start();
    }

    // ---------------------------------------------------------------- Ağ araçları

    private void fetchExternalIp() {
        if (externalIpLoading || externalIpValue == null) {
            return;
        }
        externalIpLoading = true;
        externalIpValue.setText("Getiriliyor…");
        externalIpValue.setTextColor(MUTED);
        new Thread(() -> {
            final NetworkTools.ExternalInfo info = NetworkTools.externalIp();
            runOnUiThread(() -> {
                externalIpLoading = false;
                if (info.ok()) {
                    externalIpLoaded = true;
                    externalIpValue.setText(info.ip);
                    externalIpValue.setTextColor(ACCENT);
                    Toast.makeText(this, info.summary(), Toast.LENGTH_LONG).show();
                } else {
                    externalIpLoaded = false;
                    externalIpValue.setText("Alınamadı · tekrar dene");
                    externalIpValue.setTextColor(ORANGE);
                }
            });
        }, "yek-external-ip").start();
    }

    private void runSpeedTest() {
        if (speedRunning) {
            return;
        }
        speedRunning = true;
        speedButton.setEnabled(false);
        speedButton.setAlpha(0.6f);
        speedProgress.setVisibility(View.VISIBLE);
        speedResult.setTextColor(MUTED);
        speedResult.setText("Test ediliyor… 8 MB indiriliyor");
        new Thread(() -> {
            final SpeedTestService.Result result = SpeedTestService.run();
            runOnUiThread(() -> {
                speedRunning = false;
                speedButton.setEnabled(true);
                speedButton.setAlpha(1f);
                speedProgress.setVisibility(View.GONE);
                speedResult.setText(SpeedTestService.summary(result));
                speedResult.setTextColor(result.ok() ? GREEN : RED);
            });
        }, "yek-speed").start();
    }

    private void runDnsLookup() {
        if (diagRunning) {
            return;
        }
        final String host = NetworkTools.cleanHost(diagHostInput.getText().toString());
        if (host.isEmpty()) {
            Toast.makeText(this, "Bir adres girin", Toast.LENGTH_SHORT).show();
            return;
        }
        diagRunning = true;
        diagResult.setTextColor(MUTED);
        diagResult.setText("DNS sorgulanıyor…");
        new Thread(() -> {
            final NetworkTools.DnsResult result = NetworkTools.resolve(host);
            runOnUiThread(() -> {
                diagRunning = false;
                if (!result.error.isEmpty()) {
                    diagResult.setTextColor(RED);
                    diagResult.setText(result.error + " · " + host);
                    return;
                }
                StringBuilder builder = new StringBuilder();
                builder.append(result.addresses.size()).append(" kayıt · ")
                        .append(result.elapsedMs).append(" ms");
                int shown = 0;
                for (String address : result.addresses) {
                    builder.append('\n').append(address);
                    shown++;
                    if (shown >= 4) {
                        break;
                    }
                }
                diagResult.setTextColor(GREEN);
                diagResult.setText(builder.toString());
            });
        }, "yek-dns").start();
    }

    private void runPortTest() {
        if (diagRunning) {
            return;
        }
        final String host = NetworkTools.cleanHost(diagHostInput.getText().toString());
        int port;
        try {
            port = Integer.parseInt(diagPortInput.getText().toString().trim());
        } catch (Exception error) {
            Toast.makeText(this, "Geçerli bir port girin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (host.isEmpty() || port < 1 || port > 65535) {
            Toast.makeText(this, "Adres veya port geçersiz", Toast.LENGTH_SHORT).show();
            return;
        }
        diagRunning = true;
        diagResult.setTextColor(MUTED);
        diagResult.setText("TCP " + port + " sınanıyor…");
        new Thread(() -> {
            final NetworkTools.PortResult result = NetworkTools.port(host, port);
            runOnUiThread(() -> {
                diagRunning = false;
                diagResult.setTextColor(result.open ? GREEN : RED);
                diagResult.setText(host + ":" + port + " · "
                        + (result.open ? "AÇIK" : "KAPALI / ERİŞİLEMEDİ")
                        + " · " + result.elapsedMs + " ms");
            });
        }, "yek-port").start();
    }

    // ---------------------------------------------------------------- Sensörler

    private void setSensorsEnabled(boolean enabled, boolean userAction) {
        if (enabled) {
            if (!startSensors()) {
                if (sensorSwitch != null) {
                    sensorSwitch.setChecked(false);
                }
                showSensorsOffMessage("Bu cihazda okunabilen sensör bulunamadı.");
            }
        } else {
            stopSensors();
            if (userAction) {
                showSensorsOffMessage();
            }
        }
    }

    private void restartSensorsIfNeeded() {
        if (sensorSwitch != null && sensorSwitch.isChecked() && !sensorsRunning) {
            startSensors();
        }
    }

    private boolean startSensors() {
        if (sensorManager == null || sensorList == null) {
            return false;
        }
        stopSensors();
        sensorList.removeAllViews();
        int[] types = {
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_LIGHT,
                Sensor.TYPE_PROXIMITY,
                Sensor.TYPE_PRESSURE,
                Sensor.TYPE_RELATIVE_HUMIDITY,
                Sensor.TYPE_AMBIENT_TEMPERATURE,
                Sensor.TYPE_STEP_COUNTER,
                Sensor.TYPE_ROTATION_VECTOR
        };
        for (int type : types) {
            Sensor sensor = sensorManager.getDefaultSensor(type);
            if (sensor == null || activeSensors.size() >= 10) {
                continue;
            }
            activeSensors.add(sensor);
            sensorList.addView(sensorRowView(sensor));
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        sensorsRunning = !activeSensors.isEmpty();
        if (!sensorsRunning) {
            showSensorsOffMessage("Bu cihazda okunabilen sensör bulunamadı.");
        }
        return sensorsRunning;
    }

    private void stopSensors() {
        if (sensorManager != null) {
            try {
                sensorManager.unregisterListener(this);
            } catch (Exception ignored) {
            }
        }
        sensorsRunning = false;
        activeSensors.clear();
        sensorValueViews.clear();
        sensorLastUi.clear();
    }

    private void showSensorsOffMessage() {
        showSensorsOffMessage("Canlı sensörler kapalı. Sağ üstteki anahtarı açın.");
    }

    private void showSensorsOffMessage(String message) {
        if (sensorList == null) {
            return;
        }
        sensorList.removeAllViews();
        TextView empty = text(message, 11, MUTED, false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dp(12), 0, dp(6));
        sensorList.addView(empty);
    }

    private View sensorRowView(Sensor sensor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(sensorTypeName(sensor.getType()), 12, TEXT, true);
        labels.addView(title);
        TextView name = text(sensor.getName(), 9, MUTED, false);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(name);
        row.addView(labels, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView value = text("—", 11, ACCENT, true);
        value.setGravity(Gravity.END);
        value.setMaxLines(2);
        row.addView(value, new LinearLayout.LayoutParams(0, WRAP, 1.2f));
        sensorValueViews.put(sensor.getType(), value);
        return row;
    }

    private String sensorTypeName(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return "İvmeölçer";
            case Sensor.TYPE_GYROSCOPE:
                return "Jiroskop";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "Manyetometre";
            case Sensor.TYPE_LIGHT:
                return "Işık";
            case Sensor.TYPE_PROXIMITY:
                return "Yakınlık";
            case Sensor.TYPE_PRESSURE:
                return "Basınç";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "Nem";
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "Ortam sıcaklığı";
            case Sensor.TYPE_STEP_COUNTER:
                return "Adım sayacı";
            case Sensor.TYPE_ROTATION_VECTOR:
                return "Dönüş vektörü";
            default:
                return "Sensör";
        }
    }

    private String formatSensorValue(Sensor sensor, float[] values) {
        if (values == null || values.length == 0) {
            return "—";
        }
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                return three(values, "m/s²", -200, 200);
            case Sensor.TYPE_GYROSCOPE:
                return three(values, "rad/s", -100, 100);
            case Sensor.TYPE_MAGNETIC_FIELD:
                return three(values, "µT", -10000, 10000);
            case Sensor.TYPE_ROTATION_VECTOR:
                return three(values, "", -10, 10);
            case Sensor.TYPE_LIGHT:
                return one(values[0], 0, 150000, "%.0f lx");
            case Sensor.TYPE_PROXIMITY:
                return one(values[0], 0, 1000, "%.0f cm");
            case Sensor.TYPE_PRESSURE:
                return one(values[0], 0, 1200, "%.1f hPa");
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return one(values[0], 0, 100, "%.0f %%");
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return one(values[0], -40, 125, "%.1f °C");
            case Sensor.TYPE_STEP_COUNTER:
                return one(values[0], 0, 100000000, "%.0f adım");
            default:
                return three(values, "", -1000000, 1000000);
        }
    }

    private String one(float value, double min, double max, String format) {
        if (!plausible(value, min, max)) {
            return "—";
        }
        return String.format(Locale.getDefault(), format, value);
    }

    private String three(float[] values, String unit, double min, double max) {
        StringBuilder builder = new StringBuilder();
        int count = Math.min(3, values.length);
        for (int i = 0; i < count; i++) {
            if (!plausible(values[i], min, max)) {
                return "—";
            }
            if (i > 0) {
                builder.append(" / ");
            }
            builder.append(String.format(Locale.getDefault(), "%.2f", values[i]));
        }
        if (!unit.isEmpty()) {
            builder.append(' ').append(unit);
        }
        return builder.toString();
    }

    private boolean plausible(float value, double min, double max) {
        return !Float.isNaN(value) && !Float.isInfinite(value)
                && value >= min && value <= max;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!sensorsRunning || event == null || event.sensor == null) {
            return;
        }
        TextView view = sensorValueViews.get(event.sensor.getType());
        if (view == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        Long last = sensorLastUi.get(event.sensor.getType());
        if (last != null && now - last < 350) {
            return;
        }
        sensorLastUi.put(event.sensor.getType(), now);
        view.setText(formatSensorValue(event.sensor, event.values));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
            prefs.edit().putInt(AppPrefs.K_LOW, value).apply();
            if (batteryHint != null) {
                batteryHint.setText("Uyarı eşiği: %" + alertLow + " · "
                        + (alertsEnabled ? "uyarılar açık" : "uyarılar kapalı"));
            }
        });
        highSeek = addSlider(alertsCard, "Pil üst eşiği", 70, 100, alertHigh, "%", value -> {
            alertHigh = value;
            prefs.edit().putInt(AppPrefs.K_HIGH, value).apply();
        });
        tempSeek = addSlider(alertsCard, "Sıcaklık eşiği", 35, 55, alertTemp, " °C", value -> {
            alertTemp = value;
            prefs.edit().putInt(AppPrefs.K_TEMP, value).apply();
        });

        TextView cooldown = text("Uyarılar arasında 30 dakika bekleme vardır.", 10, MUTED, false);
        cooldown.setPadding(0, dp(4), 0, 0);
        alertsCard.addView(cooldown);
        addCard(c, alertsCard);

        alertsSwitch.setOnCheckedChangeListener((button, checked) -> {
            alertsEnabled = checked;
            prefs.edit().putBoolean(AppPrefs.K_ALERTS, checked).apply();
            if (checked) {
                ensureNotifications();
            }
            if (batteryHint != null) {
                batteryHint.setText("Uyarı eşiği: %" + alertLow + " · "
                        + (checked ? "uyarılar açık" : "uyarılar kapalı"));
            }
        });

        LinearLayout behaviorCard = card();
        behaviorCard.addView(cardHeader("DAVRANIŞ", null));
        TextView refreshLabel = text("Yenileme sıklığı", 12, TEXT, false);
        behaviorCard.addView(refreshLabel);
        LinearLayout refreshRow = new LinearLayout(this);
        refreshRow.setOrientation(LinearLayout.HORIZONTAL);
        refreshRow.setPadding(0, dp(8), 0, 0);
        final int[] refreshValues = {2, 5, 10};
        for (int i = 0; i < refreshValues.length; i++) {
            final int seconds = refreshValues[i];
            TextView item = chip(seconds + " sn", refreshSeconds == seconds ? ACCENT : MUTED);
            item.setContentDescription(seconds + " saniye yenileme");
            item.setOnClickListener(v -> setRefreshSeconds(seconds));
            refreshChips[i] = item;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            if (i > 0) {
                lp.leftMargin = dp(6);
            }
            refreshRow.addView(item, lp);
        }
        behaviorCard.addView(refreshRow);

        behaviorCard.addView(divider());
        LinearLayout quietRow = new LinearLayout(this);
        quietRow.setOrientation(LinearLayout.HORIZONTAL);
        quietRow.setGravity(Gravity.CENTER_VERTICAL);
        quietRow.setPadding(0, dp(11), 0, 0);
        TextView quietLabel = text("Sessiz saatler", 12, TEXT, false);
        quietRow.addView(quietLabel, new LinearLayout.LayoutParams(0, WRAP, 1f));
        quietSwitch = new Switch(this);
        quietSwitch.setShowText(false);
        quietSwitch.setChecked(quietEnabled);
        quietSwitch.setThumbTintList(ColorStateList.valueOf(ACCENT));
        quietSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#1E2A44")));
        quietSwitch.setContentDescription("Sessiz saatler");
        quietRow.addView(quietSwitch);
        behaviorCard.addView(quietRow);

        TextView quietDesc = text("Bu aralıkta pil ve sıcaklık bildirimleri gösterilmez; "
                + "hatırlatıcılar çalışmaya devam eder.", 10, MUTED, false);
        quietDesc.setPadding(0, dp(4), 0, 0);
        quietDesc.setLineSpacing(dp(2), 1f);
        behaviorCard.addView(quietDesc);

        LinearLayout quietTimes = new LinearLayout(this);
        quietTimes.setOrientation(LinearLayout.HORIZONTAL);
        quietTimes.setPadding(0, dp(9), 0, 0);
        quietStartButton = button("", false);
        quietStartButton.setContentDescription("Sessiz saat başlangıcı");
        quietStartButton.setOnClickListener(v -> pickQuietTime(true));
        quietEndButton = button("", false);
        quietEndButton.setContentDescription("Sessiz saat bitişi");
        quietEndButton.setOnClickListener(v -> pickQuietTime(false));
        quietTimes.addView(quietStartButton, new LinearLayout.LayoutParams(0, WRAP, 1f));
        LinearLayout.LayoutParams quietEndLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        quietEndLp.leftMargin = dp(6);
        quietTimes.addView(quietEndButton, quietEndLp);
        behaviorCard.addView(quietTimes);
        updateQuietUi();

        quietSwitch.setOnCheckedChangeListener((button, checked) -> {
            quietEnabled = checked;
            prefs.edit().putBoolean(AppPrefs.K_QUIET, checked).apply();
            Toast.makeText(this, checked ? "Sessiz saatler açık" : "Sessiz saatler kapalı",
                    Toast.LENGTH_SHORT).show();
        });
        addCard(c, behaviorCard);

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

        dataCard.addView(divider());
        LinearLayout diagnosticRow = new LinearLayout(this);
        diagnosticRow.setOrientation(LinearLayout.HORIZONTAL);
        diagnosticRow.setPadding(0, dp(10), 0, 0);
        TextView copyDiag = button("TANILAMA KOPYALA", false);
        copyDiag.setContentDescription("Tanılama bilgisini kopyala");
        copyDiag.setOnClickListener(v -> copyDiagnostics());
        TextView shareDiag = button("PAYLAŞ", false);
        shareDiag.setContentDescription("Tanılama bilgisini paylaş");
        shareDiag.setOnClickListener(v -> shareDiagnostics());
        diagnosticRow.addView(copyDiag, new LinearLayout.LayoutParams(0, WRAP, 1.5f));
        LinearLayout.LayoutParams shareDiagLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        shareDiagLp.leftMargin = dp(6);
        diagnosticRow.addView(shareDiag, shareDiagLp);
        dataCard.addView(diagnosticRow);

        LinearLayout backupRow = new LinearLayout(this);
        backupRow.setOrientation(LinearLayout.HORIZONTAL);
        backupRow.setPadding(0, dp(8), 0, 0);
        TextView exportBackup = button("YEDEKLE", true);
        exportBackup.setContentDescription("Yedeği dışa aktar");
        exportBackup.setOnClickListener(v -> exportBackup());
        TextView importBackup = button("GERİ YÜKLE", false);
        importBackup.setContentDescription("Yedekten geri yükle");
        importBackup.setOnClickListener(v -> importBackup());
        backupRow.addView(exportBackup, new LinearLayout.LayoutParams(0, WRAP, 1f));
        LinearLayout.LayoutParams importLp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        importLp.leftMargin = dp(6);
        backupRow.addView(importBackup, importLp);
        dataCard.addView(backupRow);

        backupStatus = text("Yedek; ayarlar, notlar, hatırlatıcılar ve sunucu uçlarını "
                + "tek JSON dosyasında toplar. Dosya cihazınızda kalır.", 10, MUTED, false);
        backupStatus.setPadding(0, dp(9), 0, 0);
        backupStatus.setLineSpacing(dp(2), 1f);
        dataCard.addView(backupStatus);
        addCard(c, dataCard);

        LinearLayout updateCard = card();
        updateCard.addView(cardHeader("GÜNCELLEME",
                chip(BuildConfig.PLAY_BUILD ? "PLAY STORE" : "GITHUB", ACCENT)));
        TextView updateDesc = text(BuildConfig.PLAY_BUILD
                ? "Bu sürüm Google Play üzerinden güncellenir. Yeni sürümler Play Store "
                        + "uygulamasında otomatik olarak sunulur."
                : "Android sürümü GitHub Releases üzerinden denetlenir. "
                        + "Yeni sürüm bulunduğunda APK indirilir ve kurulum için Android onayı "
                        + "istenir; sessiz kurulum yapılmaz.", 11, MUTED, false);
        updateDesc.setPadding(0, dp(8), 0, dp(3));
        updateDesc.setLineSpacing(dp(3), 1f);
        updateCard.addView(updateDesc);

        updateVersionValue = rowValue();
        addInfoRow(updateCard, "Yüklü sürüm", updateVersionValue);
        updateVersionValue.setText(VERSION);
        updateLastValue = rowValue();
        addInfoRow(updateCard, "Son kontrol", updateLastValue);
        updateLastValue.setText("—");

        updateStatus = text("", 10, MUTED, false);
        updateStatus.setPadding(0, dp(11), 0, 0);
        updateStatus.setLineSpacing(dp(3), 1f);
        updateCard.addView(updateStatus);

        updateProgress = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        updateProgress.setMax(100);
        updateProgress.setProgress(0);
        updateProgress.setProgressTintList(ColorStateList.valueOf(ACCENT));
        updateProgress.setProgressBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1B2740")));
        updateProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams updateProgressLp =
                new LinearLayout.LayoutParams(MATCH, dp(6));
        updateProgressLp.topMargin = dp(8);
        updateCard.addView(updateProgress, updateProgressLp);

        LinearLayout updateButtons = new LinearLayout(this);
        updateButtons.setOrientation(LinearLayout.HORIZONTAL);
        updateButtons.setPadding(0, dp(10), 0, 0);
        updateCheckButton = button(BuildConfig.PLAY_BUILD
                ? "PLAY STORE'DA AÇ" : "GÜNCELLEME KONTROL ET", true);
        updateCheckButton.setContentDescription(BuildConfig.PLAY_BUILD
                ? "Play Store sayfasını aç" : "Güncellemeyi kontrol et");
        updateCheckButton.setOnClickListener(v -> checkForUpdates(true));
        updateInstallButton = button("KUR", false);
        updateInstallButton.setContentDescription("İndirilen güncellemeyi kur");
        updateInstallButton.setVisibility(View.GONE);
        updateInstallButton.setOnClickListener(v -> installUpdate());
        updateButtons.addView(updateCheckButton, new LinearLayout.LayoutParams(0, WRAP, 1.6f));
        LinearLayout.LayoutParams installLp = new LinearLayout.LayoutParams(0, WRAP, 0.8f);
        installLp.leftMargin = dp(6);
        updateButtons.addView(updateInstallButton, installLp);
        updateCard.addView(updateButtons);

        updateNotesButton = actionChip("SÜRÜM NOTLARINI GÖR", ACCENT);
        LinearLayout.LayoutParams notesLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        notesLp.topMargin = dp(10);
        updateNotesButton.setVisibility(View.GONE);
        updateNotesButton.setOnClickListener(v -> {
            if (pendingRelease != null && !pendingRelease.htmlUrl.isEmpty()) {
                openUrl(pendingRelease.htmlUrl);
            }
        });
        updateCard.addView(updateNotesButton, notesLp);

        LinearLayout updateAutoRow = new LinearLayout(this);
        updateAutoRow.setOrientation(LinearLayout.HORIZONTAL);
        updateAutoRow.setGravity(Gravity.CENTER_VERTICAL);
        updateAutoRow.setPadding(0, dp(11), 0, 0);
        TextView updateAutoLabel = text("Açılışta güncelleme kontrol et", 12, TEXT, false);
        updateAutoRow.addView(updateAutoLabel, new LinearLayout.LayoutParams(0, WRAP, 1f));
        updateAutoSwitch = new Switch(this);
        updateAutoSwitch.setShowText(false);
        updateAutoSwitch.setChecked(updateAuto);
        updateAutoSwitch.setThumbTintList(ColorStateList.valueOf(ACCENT));
        updateAutoSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#1E2A44")));
        updateAutoSwitch.setContentDescription("Açılışta güncelleme kontrol et");
        updateAutoSwitch.setOnCheckedChangeListener((button, checked) -> {
            updateAuto = checked;
            prefs.edit().putBoolean(AppPrefs.K_UPDATE_AUTO, checked).apply();
        });
        updateAutoRow.addView(updateAutoSwitch);
        if (!BuildConfig.PLAY_BUILD) {
            updateCard.addView(updateAutoRow);
        }

        addCard(c, updateCard);

        LinearLayout deviceCard = card();
        deviceCard.addView(cardHeader("CİHAZ BİLGİSİ", null));
        DeviceStats.Snapshot info = DeviceStats.read(this);
        TextView modelRow = rowValue();
        addInfoRow(deviceCard, "Model", modelRow);
        modelRow.setText(deviceName(info));
        TextView androidRow = rowValue();
        addInfoRow(deviceCard, "Android", androidRow);
        androidRow.setText(info.android + " · API " + info.sdk);
        TextView patchRow = rowValue();
        addInfoRow(deviceCard, "Güvenlik yaması", patchRow);
        patchRow.setText(info.securityPatch);
        TextView kernelRow = rowValue();
        addInfoRow(deviceCard, "Çekirdek", kernelRow);
        kernelRow.setText(info.kernel);
        TextView abiRow = rowValue();
        addInfoRow(deviceCard, "ABI", abiRow);
        abiRow.setText(info.abi);
        TextView screenRow = rowValue();
        addInfoRow(deviceCard, "Ekran", screenRow);
        screenRow.setText(info.screenWidth > 0
                ? info.screenWidth + " × " + info.screenHeight + " · " + info.screenDpi + " dpi"
                : "—");
        TextView refreshRateRow = rowValue();
        addInfoRow(deviceCard, "Yenileme", refreshRateRow);
        refreshRateRow.setText(info.refreshRate > 0
                ? String.format(Locale.getDefault(), "%.0f Hz", info.refreshRate) : "—");
        TextView socRow = rowValue();
        addInfoRow(deviceCard, "Yonga", socRow);
        socRow.setText(info.soc == null || info.soc.isEmpty() ? "—" : info.soc);
        TextView ramRow = rowValue();
        addInfoRow(deviceCard, "RAM", ramRow);
        ramRow.setText(DeviceStats.gb(info.ramTotal));
        TextView storageRow = rowValue();
        addInfoRow(deviceCard, "Depolama", storageRow);
        storageRow.setText(DeviceStats.gb(info.storageTotal));
        addCard(c, deviceCard);

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

        TextView aboutText = text("Mobil sistem paneli: pil, bellek, depolama, işlemci ve ağ "
                + "durumu; sunucu izleme, hız testi, DNS/port tanılama, canlı sensörler, "
                + "notlar, hatırlatıcılar ve cihaz içi yedekleme.", 11, MUTED, false);
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

    private void setRefreshSeconds(int seconds) {
        refreshSeconds = seconds;
        prefs.edit().putInt(AppPrefs.K_REFRESH, seconds).apply();
        int[] values = {2, 5, 10};
        for (int i = 0; i < refreshChips.length; i++) {
            if (refreshChips[i] != null) {
                applyChip(refreshChips[i], values[i] + " sn",
                        refreshSeconds == values[i] ? ACCENT : MUTED);
            }
        }
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        Toast.makeText(this, "Yenileme: " + seconds + " saniye", Toast.LENGTH_SHORT).show();
    }

    private void updateQuietUi() {
        if (quietStartButton != null) {
            quietStartButton.setText("BAŞLANGIÇ · " + fmtMinutes(quietStart));
        }
        if (quietEndButton != null) {
            quietEndButton.setText("BİTİŞ · " + fmtMinutes(quietEnd));
        }
    }

    private void pickQuietTime(boolean start) {
        int minutes = start ? quietStart : quietEnd;
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            int value = hourOfDay * 60 + minute;
            if (start) {
                quietStart = value;
            } else {
                quietEnd = value;
            }
            prefs.edit()
                    .putInt(start ? AppPrefs.K_QUIET_START : AppPrefs.K_QUIET_END, value)
                    .apply();
            updateQuietUi();
        }, minutes / 60, minutes % 60, true).show();
    }

    private boolean isQuietNow() {
        if (!quietEnabled) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        int now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        if (quietStart == quietEnd) {
            return false;
        }
        if (quietStart < quietEnd) {
            return now >= quietStart && now < quietEnd;
        }
        return now >= quietStart || now < quietEnd;
    }

    private String fmtMinutes(int minutes) {
        return fmtTime((minutes / 60) % 24, minutes % 60);
    }

    private void exportBackup() {
        pendingExport = SettingsBackup.export(this);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "yek-panel-yedek.json");
        startActivityForResult(intent, REQ_EXPORT_BACKUP);
    }

    private void importBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/json", "text/plain", "application/octet-stream"});
        startActivityForResult(intent, REQ_IMPORT_BACKUP);
    }

    private void readRestoreFile(Uri uri) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getContentResolver().openInputStream(uri), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && builder.length() < 4_000_000) {
                builder.append(line).append('\n');
            }
        } catch (Exception error) {
            Toast.makeText(this, "Yedek dosyası okunamadı", Toast.LENGTH_LONG).show();
            return;
        }
        pendingRestoreJson = builder.toString();
        new AlertDialog.Builder(this)
                .setTitle("Yedekten geri yüklensin mi?")
                .setMessage("Mevcut ayarlar, notlar, hatırlatıcılar ve sunucu uçları "
                        + "yedekteki verilerle değiştirilecek.")
                .setPositiveButton("Geri yükle", (dialog, which) -> applyRestore())
                .setNegativeButton("Vazgeç", (dialog, which) -> pendingRestoreJson = "")
                .show();
    }

    private void applyRestore() {
        try {
            SettingsBackup.Summary summary = SettingsBackup.importData(this, pendingRestoreJson);
            loadAlertSettings();
            loadBehaviorSettings();
            loadPcSettings();
            loadUpdateSettings();
            pcConnected = false;
            pcStatus = null;
            pcNextRetryAt = 0;
            resetPcUi();
            rebuildSettingsPage();
            refreshNotes();
            refreshReminders();
            refreshEndpointManagement();
            refreshServerPanel();
            refreshPanel();
            updateUpdateUi();
            Toast.makeText(this, "Geri yüklendi: " + summary.notes + " not · "
                    + summary.reminders + " hatırlatıcı · " + summary.endpoints + " sunucu",
                    Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            String message = error.getMessage();
            Toast.makeText(this, "Geri yükleme başarısız"
                    + (message == null || message.isEmpty() ? "" : ": " + message),
                    Toast.LENGTH_LONG).show();
        } finally {
            pendingRestoreJson = "";
        }
    }

    private void rebuildSettingsPage() {
        if (pageHost == null) {
            return;
        }
        if (settingsScroll != null) {
            pageHost.removeView(settingsScroll);
        }
        settingsScroll = buildSettingsPage();
        pageHost.addView(settingsScroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        showPage(currentPage);
    }

    private String buildDiagnostics() {
        DeviceStats.Snapshot st = DeviceStats.read(this);
        StringBuilder builder = new StringBuilder("YEK PANEL ANDROID · TANILAMA\n");
        builder.append("Tarih: ")
                .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new Date())).append('\n');
        builder.append("Cihaz: ").append(deviceName(st)).append('\n');
        builder.append("Android: ").append(st.android).append(" · API ").append(st.sdk)
                .append(" · yama ").append(st.securityPatch).append('\n');
        builder.append("Pil: %").append(st.batteryPercent)
                .append(" · ").append(st.batteryHealth)
                .append(" · ").append(String.format(Locale.getDefault(), "%.1f °C",
                        st.batteryTempC));
        if (st.batteryVoltageMv > 0) {
            builder.append(" · ").append(String.format(Locale.getDefault(), "%.2f V",
                    st.batteryVoltageMv / 1000.0));
        }
        builder.append(" · kaynak ").append(st.powerSource).append('\n');
        builder.append("CPU: ").append(st.cores).append(" çekirdek · ")
                .append(st.cpuLoad >= 0 ? "%" + st.cpuLoad : "yük kısıtlı").append('\n');
        builder.append("RAM: %").append(st.ramPercent).append(" · ")
                .append(DeviceStats.gb(st.ramTotal)).append('\n');
        builder.append("Depolama: %").append(st.storagePercent).append(" · ")
                .append(DeviceStats.gb(st.storageFree)).append(" boş\n");
        builder.append("Ağ: ").append(st.connType).append(" · ").append(st.localIp)
                .append(" · geçit ").append(st.gateway).append(" · DNS ").append(st.dns)
                .append('\n');
        builder.append("Sunucular:\n");
        List<EndpointStore.Endpoint> endpoints = endpointStore.all();
        if (endpoints.isEmpty()) {
            builder.append("  —\n");
        } else {
            for (EndpointStore.Endpoint endpoint : endpoints) {
                EndpointChecker.Result result = serverResults.get(endpoint.id);
                builder.append("  ").append(endpoint.name).append(" · ")
                        .append(endpoint.url).append(" · ")
                        .append(result == null ? "kontrol edilmedi"
                                : EndpointChecker.codeLabel(result) + " · "
                                + EndpointChecker.latencyLabel(result))
                        .append('\n');
            }
        }
        builder.append("Not: ").append(noteStore.all().size())
                .append(" · Hatırlatıcı: ").append(reminderStore.all().size())
                .append(" · Sunucu: ").append(endpoints.size()).append('\n');
        return builder.toString();
    }

    private void copyDiagnostics() {
        copyText("Tanılama", buildDiagnostics());
    }

    private void shareDiagnostics() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "YEK Panel tanılama");
        send.putExtra(Intent.EXTRA_TEXT, buildDiagnostics());
        startActivity(Intent.createChooser(send, "Tanılamayı paylaş"));
    }

    private void copyText(String label, String value) {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (manager == null) {
            Toast.makeText(this, "Pano kullanılamıyor", Toast.LENGTH_SHORT).show();
            return;
        }
        manager.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, label + " kopyalandı", Toast.LENGTH_SHORT).show();
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
        if (requestCode == REQ_LOCAL_NETWORK) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            int action = pcPendingAction;
            pcPendingAction = 0;
            if (granted) {
                Toast.makeText(this, "Yerel ağ izni verildi", Toast.LENGTH_SHORT).show();
                if (action == 1) {
                    connectPc(true);
                } else if (action == 2) {
                    discoverPc();
                }
            } else {
                setPcState("İZİN GEREKLİ", ORANGE);
                setPcStatus("Yerel ağ izni verilmedi. PC bağlantısı için Ayarlar > "
                        + "Uygulamalar > YEK Panel > İzinler bölümünden yerel ağ iznini açın.",
                        ORANGE);
                Toast.makeText(this, "Yerel ağ izni verilmedi", Toast.LENGTH_LONG).show();
            }
            return;
        }
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
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQ_IMPORT_BACKUP) {
            readRestoreFile(uri);
            return;
        }
        if (requestCode != REQ_EXPORT_TXT && requestCode != REQ_EXPORT_MD
                && requestCode != REQ_EXPORT_BACKUP) {
            return;
        }
        try (OutputStream raw = getContentResolver().openOutputStream(uri, "wt")) {
            if (raw == null) {
                throw new IllegalStateException("Çıktı akışı açılamadı");
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(raw, StandardCharsets.UTF_8)) {
                writer.write(pendingExport);
            }
            Toast.makeText(this, requestCode == REQ_EXPORT_BACKUP
                    ? "Yedek dışa aktarıldı" : "Notlar dışa aktarıldı", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, requestCode == REQ_EXPORT_BACKUP
                    ? "Yedek dışa aktarılamadı" : "Dışa aktarma başarısız",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- Güncelleme

    private void maybeAutoCheckUpdate() {
        if (BuildConfig.PLAY_BUILD || !updateAuto) {
            return;
        }
        long last = prefs.getLong(AppPrefs.K_UPDATE_LAST, 0);
        if (last > 0 && System.currentTimeMillis() - last < AppPrefs.UPDATE_INTERVAL) {
            return;
        }
        handler.postDelayed(() -> checkForUpdates(false), 1800L);
    }

    private void checkForUpdates(boolean userAction) {
        if (BuildConfig.PLAY_BUILD) {
            setUpdateStatus("Güncellemeler Google Play üzerinden yapılır.", GREEN);
            updateUpdateUi();
            if (userAction) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + getPackageName())));
                } catch (Exception ignored) {
                    openUrl("https://play.google.com/store/apps/details?id=" + getPackageName());
                }
            }
            return;
        }
        if (updateChecking) {
            return;
        }
        if (!userAction) {
            long last = prefs.getLong(AppPrefs.K_UPDATE_LAST, 0);
            if (last > 0 && System.currentTimeMillis() - last < AppPrefs.UPDATE_INTERVAL) {
                updateUpdateUi();
                return;
            }
        }
        updateChecking = true;
        if (updateCheckButton != null) {
            updateCheckButton.setEnabled(false);
            updateCheckButton.setText("KONTROL EDİLİYOR…");
        }
        setUpdateStatus("Güncelleme kontrol ediliyor…", ORANGE);
        updateUpdateUi();
        new Thread(() -> {
            UpdateChecker.Release release = null;
            String error = "";
            try {
                release = UpdateChecker.check(MainActivity.this);
            } catch (Exception exception) {
                error = shortError(exception);
            }
            final UpdateChecker.Release found = release;
            final String failure = error;
            handler.post(() -> {
                updateChecking = false;
                if (updateCheckButton != null) {
                    updateCheckButton.setEnabled(true);
                    updateCheckButton.setText("GÜNCELLEME KONTROL ET");
                }
                if (!failure.isEmpty()) {
                    setUpdateStatus("Kontrol edilemedi: " + failure, RED);
                    if (userAction) {
                        Toast.makeText(MainActivity.this,
                                "Güncelleme kontrol edilemedi", Toast.LENGTH_LONG).show();
                    }
                    updateUpdateUi();
                    return;
                }
                prefs.edit().putLong(AppPrefs.K_UPDATE_LAST,
                        System.currentTimeMillis()).apply();
                if (found == null) {
                    setUpdateStatus("En son sürümdesiniz · v" + VERSION, GREEN);
                    if (userAction) {
                        Toast.makeText(MainActivity.this, "Uygulama güncel",
                                Toast.LENGTH_SHORT).show();
                    }
                    updateUpdateUi();
                    return;
                }
                pendingRelease = found;
                setUpdateStatus("Yeni sürüm hazır: v" + found.version, ACCENT);
                updateUpdateUi();
                showUpdateDialog(found);
            });
        }, "yek-update-check").start();
    }

    private void showUpdateDialog(UpdateChecker.Release release) {
        StringBuilder message = new StringBuilder();
        message.append(release.notes);
        message.append("\n\nYüklü sürüm: v").append(VERSION);
        if (release.apkSize > 0) {
            message.append("\nAPK boyutu: ").append(PcClient.bytes(release.apkSize));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("YEK Panel Android " + release.version)
                .setMessage(message.toString())
                .setPositiveButton("İNDİR VE KUR",
                        (dialog, which) -> startUpdateDownload(release))
                .setNegativeButton("DAHA SONRA", null);
        if (release.htmlUrl != null && !release.htmlUrl.isEmpty()) {
            builder.setNeutralButton("GITHUB'DA AÇ",
                    (dialog, which) -> openUrl(release.htmlUrl));
        }
        builder.show();
    }

    private void startUpdateDownload(UpdateChecker.Release release) {
        if (release == null) {
            return;
        }
        try {
            long id = UpdateChecker.download(this, release);
            if (id < 0) {
                setUpdateStatus("İndirme başlatılamadı", RED);
                Toast.makeText(this, "İndirme başlatılamadı", Toast.LENGTH_LONG).show();
                return;
            }
            updateDownloadId = id;
            updateDownloading = true;
            setUpdateProgress(0);
            if (updateInstallButton != null) {
                updateInstallButton.setVisibility(View.GONE);
            }
            setUpdateStatus("Güncelleme indiriliyor…", ORANGE);
            updateUpdateUi();
            handler.removeCallbacks(updatePoll);
            handler.post(updatePoll);
            Toast.makeText(this, "Güncelleme indiriliyor", Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            setUpdateStatus("İndirme başlatılamadı: " + shortError(exception), RED);
            updateUpdateUi();
        }
    }

    private void finishUpdateDownload(boolean success) {
        updateDownloading = false;
        handler.removeCallbacks(updatePoll);
        if (!success) {
            setUpdateStatus("Güncelleme indirilemedi · tekrar deneyin", RED);
            updateUpdateUi();
            Toast.makeText(this, "Güncelleme indirilemedi", Toast.LENGTH_LONG).show();
            return;
        }
        setUpdateProgress(100);
        setUpdateStatus("Güncelleme indirildi · KUR düğmesine dokunun", GREEN);
        updateUpdateUi();
        if (updateInstallButton != null) {
            updateInstallButton.setVisibility(View.VISIBLE);
        }
        new AlertDialog.Builder(this)
                .setTitle("Güncelleme indirildi")
                .setMessage("YEK Panel" + (pendingRelease == null ? ""
                        : " v" + pendingRelease.version)
                        + " kurulmaya hazır. Android kurulum onayı istenecek.")
                .setPositiveButton("ŞİMDİ KUR", (dialog, which) -> installUpdate())
                .setNegativeButton("SONRA", null)
                .show();
    }

    private void installUpdate() {
        if (!UpdateChecker.isDownloaded(this)) {
            Toast.makeText(this, "Önce güncellemeyi indirin", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean opened = UpdateChecker.install(this);
        if (!opened) {
            if (!UpdateChecker.canInstall(this)) {
                Toast.makeText(this, "Kurulum iznini açın, sonra KUR düğmesine "
                        + "tekrar dokunun", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Kurulum ekranı açılamadı", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setUpdateStatus(String message, int color) {
        updateMessage = message;
        updateMessageColor = color;
        if (updateStatus != null) {
            updateStatus.setText(message);
            updateStatus.setTextColor(color);
        }
    }

    private void setUpdateProgress(int percent) {
        if (updateProgress != null) {
            updateProgress.setProgress(Math.max(0, Math.min(100, percent)));
            updateProgress.setVisibility(View.VISIBLE);
        }
    }

    private void updateUpdateUi() {
        if (updateVersionValue != null) {
            updateVersionValue.setText(VERSION);
        }
        long last = prefs.getLong(AppPrefs.K_UPDATE_LAST, 0);
        if (updateLastValue != null) {
            if (last > 0) {
                updateLastValue.setText(new SimpleDateFormat("dd.MM.yyyy HH:mm",
                        Locale.getDefault()).format(new Date(last)));
            } else {
                updateLastValue.setText("—");
            }
        }
        if (updateStatus != null) {
            updateStatus.setText(updateMessage);
            updateStatus.setTextColor(updateMessageColor);
        }
        if (updateAutoSwitch != null) {
            updateAutoSwitch.setChecked(updateAuto);
        }
        if (updateCheckButton != null) {
            updateCheckButton.setEnabled(!updateChecking && !updateDownloading);
        }
        if (updateInstallButton != null) {
            updateInstallButton.setVisibility(UpdateChecker.isDownloaded(this)
                    ? View.VISIBLE : View.GONE);
        }
        if (updateNotesButton != null) {
            updateNotesButton.setVisibility(pendingRelease != null
                    && !pendingRelease.htmlUrl.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (updateProgress != null) {
            updateProgress.setVisibility(updateDownloading ? View.VISIBLE : View.GONE);
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception exception) {
            Toast.makeText(this, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private String shortError(Throwable error) {
        if (error == null) {
            return "Bilinmeyen hata";
        }
        String message = error.getMessage() == null ? "" : error.getMessage().trim();
        String lower = message.toLowerCase(Locale.ROOT);
        if (error instanceof UnknownHostException
                || lower.contains("unable to resolve host")
                || lower.contains("no address associated")) {
            return "Sunucuya ulaşılamadı. İnternet bağlantısını kontrol edin.";
        }
        if (error instanceof SocketTimeoutException) {
            return "Sunucu yanıt vermedi (zaman aşımı)";
        }
        if (error instanceof ConnectException) {
            return "Sunucuya bağlanılamadı. İnternet bağlantısını kontrol edin.";
        }
        if (lower.contains("cleartext")) {
            return "Güvenli olmayan bağlantı engellendi";
        }
        if (message.isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return message.length() > 120 ? message.substring(0, 120) + "…" : message;
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
