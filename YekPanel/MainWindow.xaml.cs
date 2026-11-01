using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Shapes;
using System.Windows.Threading;
using Microsoft.Win32;
using YekPanel.Models;
using YekPanel.Services;
using Drawing = System.Drawing;
using Forms = System.Windows.Forms;

namespace YekPanel;

public partial class MainWindow : Window
{
    private static readonly CultureInfo Tr = new("tr-TR");

    private static readonly Brush TextSoft = MakeBrush(0xEA, 0xF4, 0xFF);
    private static readonly Brush Muted = MakeBrush(0x7F, 0x9A, 0xBE);
    private static readonly Brush StatusGreen = MakeBrush(0x34, 0xD3, 0x99);
    private static readonly Brush StatusAmber = MakeBrush(0xFB, 0xBF, 0x24);
    private static readonly Brush StatusRed = MakeBrush(0xF8, 0x71, 0x71);
    private static readonly Brush StatusGray = MakeBrush(0x3C, 0x5A, 0x7D);
    private static readonly Brush RowBg = MakeBrush(0x0A, 0x1E, 0x38);
    private static readonly Brush RowBorder = MakeBrush(0x14, 0x36, 0x5D);
    private static readonly Brush GridLine = MakeBrush(0x14, 0x36, 0x5D);

    private Brush _accent = MakeBrush(0x4C, 0xC9, 0xF0);
    private Brush _accentText = MakeBrush(0x7F, 0xD6, 0xF7);
    private Brush _accentDeep = MakeBrush(0x2F, 0x7D, 0xF6);
    private Brush _barNormal = CreateBarBrush(0x2F, 0x7D, 0xF6, 0x4C, 0xC9, 0xF0);
    private Brush _cpuLine = MakeBrush(0x4C, 0xC9, 0xF0);
    private Brush _cpuFill = MakeBrush(0x4C, 0xC9, 0xF0, 46);
    private Brush _ramLine = MakeBrush(0x2F, 0x7D, 0xF6);
    private Brush _ramFill = MakeBrush(0x2F, 0x7D, 0xF6, 46);
    private Brush _latencyLine = MakeBrush(0x7D, 0xD3, 0xFC);

    private const int HistoryLength = 60;
    private const int ServerHistoryLength = 20;
    private const int LatencyHistoryLength = 20;
    private const double FullHeight = 900;
    private const double MiniHeight = 200;

    private readonly SystemMonitor _system = new();
    private readonly NetworkMeter _meter = new();
    private readonly ProcessMonitor _processMonitor = new();
    private readonly HardwareSensorService _sensors = new();
    private readonly DispatcherTimer _processTimer = new() { Interval = TimeSpan.FromSeconds(2) };
    private readonly DispatcherTimer _temperatureTimer = new() { Interval = TimeSpan.FromSeconds(8) };
    private readonly List<ProcessRow> _processRows = new();
    private Forms.NotifyIcon? _tray;
    private double _lastCpuPercent;
    private double _lastRamPercent;
    private double _lastDiskPercent;
    private string _serverSummaryText = "kontrol ediliyor";
    private string _selectedAccent = "sky";
    private bool _miniMode;
    private bool _trayHintShown;
    private bool _speedTesting;
    private readonly HttpClient _http = new(new SocketsHttpHandler
    {
        ConnectTimeout = TimeSpan.FromSeconds(6),
        AutomaticDecompression = System.Net.DecompressionMethods.All
    })
    {
        Timeout = TimeSpan.FromSeconds(10)
    };

    private readonly List<ServerRow> _rows = new();
    private readonly DispatcherTimer _clockTimer = new() { Interval = TimeSpan.FromSeconds(1) };
    private readonly DispatcherTimer _serverTimer;
    private readonly AppSettings _settings;
    private readonly Queue<double> _cpuHistory = new();
    private readonly Queue<double> _ramHistory = new();
    private readonly DispatcherTimer _weatherTimer = new() { Interval = TimeSpan.FromMinutes(10) };
    private readonly DispatcherTimer _netTimer = new() { Interval = TimeSpan.FromSeconds(20) };
    private readonly DispatcherTimer _stopwatchTimer = new() { Interval = TimeSpan.FromMilliseconds(100) };
    private readonly DispatcherTimer _countdownTimer = new() { Interval = TimeSpan.FromSeconds(1) };
    private readonly DispatcherTimer _summaryTimer = new() { Interval = TimeSpan.FromSeconds(30) };
    private readonly DispatcherTimer _connectionTimer = new() { Interval = TimeSpan.FromSeconds(30) };
    private readonly Stopwatch _stopwatch = new();
    private readonly HashSet<string> _sslAlerted = new();
    private WeatherInfo? _weather;
    private bool _weatherLoading;
    private bool _checking;
    private bool _ipLoading;
    private bool _connectionLoading;
    private DateTime? _countdownEnd;
    private DateTime _lastConnectionRefresh = DateTime.MinValue;
    private DateTime _lastPublicIpCheck = DateTime.MinValue;
    private string _lastLocalIp = "";
    private string _lastPublicIp = "";
    private double _pendingOpacity = 1.0;
    private int _reportDays = 7;
    private string _publicIp = "—";

    public MainWindow()
    {
        InitializeComponent();
        _settings = SettingsStore.Load();
        ApplyAccent(_settings.Accent, persist: false);
        Opacity = _settings.WindowOpacity;
        _pendingOpacity = _settings.WindowOpacity;

        if (_settings.WindowLeft is double savedLeft && _settings.WindowTop is double savedTop)
        {
            WindowStartupLocation = WindowStartupLocation.Manual;
            Left = savedLeft;
            Top = savedTop;
            EnsureOnScreen();
        }

        Topmost = _settings.Topmost;
        UpdatePinVisual();
        InitializeTray();

        _serverTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(_settings.RefreshSeconds) };
        _serverTimer.Tick += async (_, _) => await CheckServersAsync();

        BuildServerRows();
        BuildProcessRows();

        _clockTimer.Tick += (_, _) => UpdateClock();
        _clockTimer.Start();
        UpdateClock();
        UpdateSystem();
        UpdateProcesses();

        _weatherTimer.Tick += async (_, _) => await RefreshWeatherAsync();
        _netTimer.Tick += async (_, _) => await UpdateInternetAsync();
        _processTimer.Tick += (_, _) => UpdateProcesses();
        _temperatureTimer.Tick += async (_, _) => await UpdateTemperaturesAsync();
        _stopwatchTimer.Tick += (_, _) => UpdateStopwatch();
        _countdownTimer.Tick += (_, _) => UpdateCountdown();
        _summaryTimer.Tick += (_, _) => CheckDailySummary();
        _connectionTimer.Tick += async (_, _) => await RefreshConnectionAsync();
        CpuChart.SizeChanged += (_, _) => RenderSpark(CpuChart, _cpuHistory, _cpuLine, _cpuFill);
        RamChart.SizeChanged += (_, _) => RenderSpark(RamChart, _ramHistory, _ramLine, _ramFill);

        _serverTimer.Start();
        _weatherTimer.Start();
        _netTimer.Start();
        _processTimer.Start();
        _temperatureTimer.Start();
        _summaryTimer.Start();
        _connectionTimer.Start();
        Loaded += async (_, _) =>
        {
            UpdateSystem();
            await CheckServersAsync();
            _ = UpdateInternetAsync();
            _ = RefreshWeatherAsync();
            _ = UpdateTemperaturesAsync();
            _ = RefreshPublicIpAsync();
            _ = RefreshConnectionAsync(force: true);

            if (_settings.RememberMini && _settings.MiniMode)
                ToggleMini();

            if (_settings.StartHidden)
            {
                Hide();
                ShowTrayHint();
            }
        };
    }

    private static SolidColorBrush MakeBrush(byte r, byte g, byte b, byte alpha = 255)
    {
        var brush = new SolidColorBrush(Color.FromArgb(alpha, r, g, b));
        brush.Freeze();
        return brush;
    }

    private static Brush CreateBarBrush(byte r1, byte g1, byte b1, byte r2, byte g2, byte b2)
    {
        var brush = new LinearGradientBrush(
            Color.FromRgb(r1, g1, b1),
            Color.FromRgb(r2, g2, b2),
            new Point(0, 0),
            new Point(1, 0));
        brush.Freeze();
        return brush;
    }

    private void ApplyAccent(string accent, bool persist)
    {
        var (light, text, deep) = accent switch
        {
            "teal" => (Color.FromRgb(0x2D, 0xD4, 0xBF), Color.FromRgb(0x5E, 0xEA, 0xD4), Color.FromRgb(0x0D, 0x94, 0x88)),
            "green" => (Color.FromRgb(0x4A, 0xDE, 0x80), Color.FromRgb(0x86, 0xEF, 0xAC), Color.FromRgb(0x16, 0xA3, 0x4A)),
            "amber" => (Color.FromRgb(0xFB, 0xBF, 0x24), Color.FromRgb(0xFC, 0xD3, 0x4D), Color.FromRgb(0xD9, 0x77, 0x06)),
            _ => (Color.FromRgb(0x4C, 0xC9, 0xF0), Color.FromRgb(0x7F, 0xD6, 0xF7), Color.FromRgb(0x2F, 0x7D, 0xF6))
        };

        _accent = Freeze(new SolidColorBrush(light));
        _accentText = Freeze(new SolidColorBrush(text));
        _accentDeep = Freeze(new SolidColorBrush(deep));
        _barNormal = Freeze(new LinearGradientBrush(deep, light, new Point(0, 0), new Point(1, 0)));
        _cpuLine = Freeze(new SolidColorBrush(light));
        _cpuFill = Freeze(new SolidColorBrush(Color.FromArgb(46, light.R, light.G, light.B)));
        _ramLine = Freeze(new SolidColorBrush(deep));
        _ramFill = Freeze(new SolidColorBrush(Color.FromArgb(46, deep.R, deep.G, deep.B)));
        _latencyLine = Freeze(new SolidColorBrush(text));

        Resources["AccentBar"] = _barNormal;
        Resources["AccentButtonBrush"] = Freeze(new LinearGradientBrush(deep, light, new Point(0, 0), new Point(1, 0)));
        Resources["AccentText"] = _accentText;
        Resources["AccentSoft"] = _accent;
        Resources["AccentDeep"] = _accentDeep;

        _selectedAccent = accent;
        UpdatePinVisual();

        RenderSpark(CpuChart, _cpuHistory, _cpuLine, _cpuFill);
        RenderSpark(RamChart, _ramHistory, _ramLine, _ramFill);

        foreach (var row in _rows)
            RenderLatency(row.LatencyChart, row.Latencies);

        DiskBar.Foreground = BarColor(DiskBar.Value);
        MiniCpuBar.Foreground = BarColor(MiniCpuBar.Value);
        MiniRamBar.Foreground = BarColor(MiniRamBar.Value);
        UpdateAccentSwatches();

        if (persist)
        {
            _settings.Accent = accent;
            SettingsStore.Save(_settings);
        }
    }

    private static Brush Freeze(Brush brush)
    {
        brush.Freeze();
        return brush;
    }

    private void UpdateAccentSwatches()
    {
        foreach (var (border, key) in new[]
                 {
                     (SwatchSky, "sky"),
                     (SwatchTeal, "teal"),
                     (SwatchGreen, "green"),
                     (SwatchAmber, "amber")
                 })
        {
            border.BorderBrush = key == _selectedAccent ? Brushes.White : Brushes.Transparent;
        }
    }

    private void UpdateClock()
    {
        var now = DateTime.Now;
        var format = _settings.Clock24Hour
            ? (_settings.ShowSeconds ? "HH:mm:ss" : "HH:mm")
            : (_settings.ShowSeconds ? "hh:mm:ss tt" : "hh:mm tt");

        ClockText.Text = now.ToString(format, Tr);
        DateText.Text = now.ToString("dd MMMM yyyy dddd", Tr);
        MiniClock.Text = ClockText.Text;
        MiniDate.Text = DateText.Text;
    }

    private void UpdateSystem()
    {
        var snapshot = _system.Read();
        MachineText.Text = snapshot.MachineName;

        _lastCpuPercent = snapshot.CpuPercent;
        CpuValue.Text = $"%{snapshot.CpuPercent.ToString("0", Tr)}";
        MiniCpuValue.Text = CpuValue.Text;
        MiniCpuBar.Value = snapshot.CpuPercent;
        MiniCpuBar.Foreground = BarColor(snapshot.CpuPercent);
        AddHistory(_cpuHistory, snapshot.CpuPercent);
        RenderSpark(CpuChart, _cpuHistory, _cpuLine, _cpuFill);

        var totalGb = snapshot.TotalRamBytes / 1024d / 1024d / 1024d;
        var freeGb = snapshot.AvailableRamBytes / 1024d / 1024d / 1024d;
        var usedGb = totalGb - freeGb;
        var ramPercent = totalGb > 0 ? usedGb / totalGb * 100 : 0;
        _lastRamPercent = ramPercent;
        RamValue.Text = $"{usedGb.ToString("0.0", Tr)} / {totalGb.ToString("0.0", Tr)} GB (%{ramPercent.ToString("0", Tr)})";
        MiniRamValue.Text = $"%{ramPercent.ToString("0", Tr)}";
        MiniRamBar.Value = ramPercent;
        MiniRamBar.Foreground = BarColor(ramPercent);
        AddHistory(_ramHistory, ramPercent);
        RenderSpark(RamChart, _ramHistory, _ramLine, _ramFill);

        if (snapshot.DiskTotalBytes > 0)
        {
            var diskTotal = snapshot.DiskTotalBytes / 1024d / 1024d / 1024d;
            var diskFree = snapshot.DiskFreeBytes / 1024d / 1024d / 1024d;
            var diskUsed = diskTotal - diskFree;
            var diskPercent = diskUsed / diskTotal * 100;
            _lastDiskPercent = diskPercent;
            DiskValue.Text = $"{diskUsed.ToString("0", Tr)} / {diskTotal.ToString("0", Tr)} GB (%{diskPercent.ToString("0", Tr)})";
            DiskBar.Value = diskPercent;
            DiskBar.Foreground = BarColor(diskPercent);
        }
        else
        {
            DiskValue.Text = "—";
            DiskBar.Value = 0;
            _lastDiskPercent = 0;
        }

        var uptime = snapshot.Uptime;
        UptimeText.Text = uptime.TotalDays >= 1
            ? $"{(int)uptime.TotalDays} gün {uptime.Hours:00}:{uptime.Minutes:00}"
            : $"{uptime.Hours:00}:{uptime.Minutes:00}:{uptime.Seconds:00}";

        var (down, up) = _meter.Sample();
        DownloadText.Text = FormatRate(down);
        UploadText.Text = FormatRate(up);
        MiniSpeedText.Text = $"↓ {FormatRate(down)}  ↑ {FormatRate(up)}";
        UpdateTrayText();
    }

    private void UpdateTrayText()
    {
        if (_tray == null)
            return;

        var text = $"YEK Panel · CPU %{_lastCpuPercent.ToString("0", Tr)} · {_serverSummaryText}";
        if (text.Length > 63)
            text = text[..63];

        try
        {
            _tray.Text = text;
        }
        catch
        {
        }
    }

    private static string FormatRate(double bytesPerSecond) => bytesPerSecond switch
    {
        < 1024 => $"{bytesPerSecond.ToString("0", Tr)} B/s",
        < 1024 * 1024 => $"{(bytesPerSecond / 1024).ToString("0.0", Tr)} KB/s",
        _ => $"{(bytesPerSecond / 1024 / 1024).ToString("0.0", Tr)} MB/s"
    };

    private double ConvertTemp(double celsius) =>
        _settings.UseFahrenheit ? celsius * 9 / 5 + 32 : celsius;

    private string TempNumber(double celsius) =>
        $"{ConvertTemp(celsius).ToString("0", Tr)}°";

    private string TempWithUnit(double celsius) =>
        $"{ConvertTemp(celsius).ToString("0", Tr)}°{(_settings.UseFahrenheit ? "F" : "C")}";

    private Brush BarColor(double percent) =>
        percent >= 90 ? StatusRed : percent >= 70 ? StatusAmber : _barNormal;

    private static void AddHistory(Queue<double> history, double value)
    {
        var clamped = Math.Clamp(value, 0, 100);
        if (history.Count == 0)
            for (int i = 0; i < HistoryLength - 1; i++)
                history.Enqueue(clamped);
        history.Enqueue(clamped);
        while (history.Count > HistoryLength)
            history.Dequeue();
    }

    private static void RenderSpark(Canvas canvas, IEnumerable<double> history, Brush lineBrush, Brush fillBrush)
    {
        if (canvas.ActualWidth < 12 || canvas.ActualHeight < 8)
            return;

        var values = history.ToArray();
        canvas.Children.Clear();

        double width = canvas.ActualWidth;
        double height = canvas.ActualHeight;

        foreach (var fraction in new[] { 0.33, 0.66 })
        {
            canvas.Children.Add(new Line
            {
                X1 = 0,
                X2 = width,
                Y1 = height * fraction,
                Y2 = height * fraction,
                Stroke = GridLine,
                StrokeThickness = 0.6
            });
        }

        if (values.Length < 2)
            return;

        var points = new PointCollection();
        for (int i = 0; i < values.Length; i++)
        {
            var x = width * i / (HistoryLength - 1);
            var y = height - 2 - Math.Clamp(values[i], 0, 100) / 100.0 * (height - 4);
            points.Add(new Point(x, y));
        }

        var area = new PointCollection(points)
        {
            new Point(points[^1].X, height),
            new Point(0, height)
        };

        canvas.Children.Add(new Polygon { Points = area, Fill = fillBrush });
        canvas.Children.Add(new Polyline
        {
            Points = points,
            Stroke = lineBrush,
            StrokeThickness = 1.8,
            StrokeLineJoin = PenLineJoin.Round
        });
    }

    private void BuildProcessRows()
    {
        ProcessesList.Children.Clear();
        _processRows.Clear();

        for (int i = 0; i < 3; i++)
        {
            var name = new TextBlock
            {
                FontSize = 10.5,
                Foreground = TextSoft,
                TextTrimming = TextTrimming.CharacterEllipsis,
                VerticalAlignment = VerticalAlignment.Center
            };

            var value = new TextBlock
            {
                FontFamily = new FontFamily("Consolas"),
                FontSize = 10,
                Foreground = Muted,
                HorizontalAlignment = HorizontalAlignment.Right,
                VerticalAlignment = VerticalAlignment.Center,
                Margin = new Thickness(8, 0, 0, 0)
            };

            var grid = new Grid { Margin = new Thickness(0, i == 0 ? 0 : 4, 0, 0) };
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
            grid.Children.Add(name);
            Grid.SetColumn(value, 1);
            grid.Children.Add(value);

            ProcessesList.Children.Add(grid);
            _processRows.Add(new ProcessRow(name, value));
        }
    }

    private void UpdateProcesses()
    {
        var top = _processMonitor.TopByMemory(_processRows.Count);

        for (int i = 0; i < _processRows.Count; i++)
        {
            var row = _processRows[i];
            if (i < top.Count)
            {
                var sample = top[i];
                row.Name.Text = sample.Name;
                row.Value.Text = $"{sample.MemoryMb.ToString("0", Tr)} MB · %{sample.CpuPercent.ToString("0", Tr)}";
            }
            else
            {
                row.Name.Text = "—";
                row.Value.Text = "";
            }
        }
    }

    private void BuildServerRows()
    {
        _rows.Clear();
        ServerList.Children.Clear();

        foreach (var endpoint in _settings.Endpoints)
        {
            var row = CreateRow(endpoint);
            _rows.Add(row);
            ServerList.Children.Add(row.Root);
        }
    }

    private ServerRow CreateRow(EndpointConfig endpoint)
    {
        var dot = new Ellipse
        {
            Width = 8,
            Height = 8,
            Fill = StatusGray,
            VerticalAlignment = VerticalAlignment.Center,
            Margin = new Thickness(0, 1, 10, 0)
        };

        var name = new TextBlock
        {
            Text = endpoint.Name,
            FontSize = 12.5,
            FontWeight = FontWeights.SemiBold,
            Foreground = TextSoft,
            TextTrimming = TextTrimming.CharacterEllipsis
        };

        var host = new TextBlock
        {
            Text = DescribeHost(endpoint.Url),
            FontSize = 9.5,
            Foreground = Muted,
            Margin = new Thickness(0, 2, 0, 0),
            TextTrimming = TextTrimming.CharacterEllipsis
        };

        var labels = new StackPanel { VerticalAlignment = VerticalAlignment.Center };
        labels.Children.Add(name);
        labels.Children.Add(host);

        var latency = new TextBlock
        {
            Text = "—",
            FontFamily = new FontFamily("Consolas"),
            FontSize = 11,
            Foreground = TextSoft,
            HorizontalAlignment = HorizontalAlignment.Right
        };

        var status = new TextBlock
        {
            Text = string.IsNullOrWhiteSpace(endpoint.Url)
                ? "ADRES YOK"
                : endpoint.Maintenance ? "BAKIMDA" : "BEKLİYOR",
            FontSize = 8.5,
            FontWeight = FontWeights.SemiBold,
            Foreground = Muted,
            HorizontalAlignment = HorizontalAlignment.Right,
            Margin = new Thickness(0, 2, 0, 0)
        };

        var right = new StackPanel
        {
            VerticalAlignment = VerticalAlignment.Center,
            HorizontalAlignment = HorizontalAlignment.Right
        };
        right.Children.Add(latency);
        right.Children.Add(status);

        var grid = new Grid();
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

        grid.Children.Add(dot);
        Grid.SetColumn(labels, 1);
        grid.Children.Add(labels);
        Grid.SetColumn(right, 2);
        grid.Children.Add(right);

        var historyPanel = new StackPanel
        {
            Orientation = Orientation.Horizontal,
            Margin = new Thickness(0, 5, 0, 0)
        };

        var historyBars = new List<Border>();
        for (int i = 0; i < ServerHistoryLength; i++)
        {
            var bar = new Border
            {
                Width = 4,
                Height = 4,
                CornerRadius = new CornerRadius(1),
                Background = StatusGray,
                Margin = new Thickness(0, 0, 2, 0)
            };
            historyBars.Add(bar);
            historyPanel.Children.Add(bar);
        }

        var latencyChart = new Canvas
        {
            Width = 62,
            Height = 14,
            HorizontalAlignment = HorizontalAlignment.Right,
            VerticalAlignment = VerticalAlignment.Center,
            ToolTip = "Son yanıt süreleri"
        };

        var wol = new Button
        {
            Content = "\uE7E8",
            FontFamily = new FontFamily("Segoe MDL2 Assets"),
            FontSize = 12,
            Foreground = StatusGreen,
            Style = (Style)FindResource("ChipButton"),
            Width = 26,
            Height = 20,
            Padding = new Thickness(0),
            Margin = new Thickness(0, 0, 6, 0),
            VerticalAlignment = VerticalAlignment.Center,
            ToolTip = "Wake-on-LAN ile aç",
            Visibility = Visibility.Collapsed
        };
        System.Windows.Automation.AutomationProperties.SetName(wol, endpoint.Name + " sunucusunu aç");
        wol.Click += (_, _) => _ = WakeServerAsync(endpoint.Name, endpoint.Mac);

        var bottomRight = new StackPanel
        {
            Orientation = Orientation.Horizontal,
            HorizontalAlignment = HorizontalAlignment.Right,
            VerticalAlignment = VerticalAlignment.Center
        };
        bottomRight.Children.Add(wol);
        bottomRight.Children.Add(latencyChart);

        var bottom = new Grid();
        bottom.Children.Add(historyPanel);
        bottom.Children.Add(bottomRight);

        var content = new StackPanel();
        content.Children.Add(grid);
        content.Children.Add(bottom);

        var root = new Border
        {
            Background = RowBg,
            BorderBrush = RowBorder,
            BorderThickness = new Thickness(1),
            CornerRadius = new CornerRadius(9),
            Padding = new Thickness(10, 8, 10, 8),
            Margin = new Thickness(0, 0, 0, 6),
            Child = content,
            ToolTip = string.IsNullOrWhiteSpace(endpoint.Url) ? "Adres girilmedi" : endpoint.Url,
            Opacity = string.IsNullOrWhiteSpace(endpoint.Url) ? 0.6 : 1.0
        };

        return new ServerRow
        {
            Root = root,
            Dot = dot,
            Latency = latency,
            Status = status,
            Name = endpoint.Name,
            Url = endpoint.Url,
            Mac = endpoint.Mac,
            Wol = wol,
            Maintenance = endpoint.Maintenance,
            HistoryBars = historyBars,
            LatencyChart = latencyChart
        };
    }

    private static string DescribeHost(string url)
    {
        if (string.IsNullOrWhiteSpace(url))
            return "adres girilmedi";

        return Uri.TryCreate(url, UriKind.Absolute, out var uri)
            ? uri.Host + (uri.IsDefaultPort ? "" : ":" + uri.Port)
            : url;
    }

    private async Task CheckServersAsync()
    {
        if (_checking)
            return;

        _checking = true;
        RefreshButton.IsEnabled = false;

        try
        {
            ServerSummary.Text = "KONTROL EDİLİYOR…";
            ServerSummary.Foreground = _accent;

            foreach (var row in _rows.Where(r => !string.IsNullOrWhiteSpace(r.Url)))
            {
                row.Status.Text = "KONTROL…";
                row.Status.Foreground = Muted;
            }

            var results = await Task.WhenAll(
                _settings.Endpoints.Select(e => ServerMonitor.CheckAsync(_http, e.Url, CancellationToken.None)));

            for (int i = 0; i < _rows.Count && i < results.Length; i++)
                ApplyResult(_rows[i], results[i]);

            var configured = 0;
            var active = 0;
            for (int i = 0; i < _settings.Endpoints.Count && i < results.Length; i++)
            {
                if (string.IsNullOrWhiteSpace(_settings.Endpoints[i].Url))
                    continue;

                configured++;
                if (results[i].Responded && results[i].StatusCode < 500)
                    active++;
            }

            if (configured == 0)
            {
                ServerSummary.Text = "ADRES YOK";
                ServerSummary.Foreground = Muted;
                _serverSummaryText = "adres yok";
                MiniServerText.Text = "ADRES YOK";
                MiniServerText.Foreground = Muted;
                MiniServerDot.Fill = StatusGray;
            }
            else
            {
                var summaryBrush = active == configured ? StatusGreen : active > 0 ? StatusAmber : StatusRed;
                ServerSummary.Text = $"{active}/{configured} AKTİF";
                ServerSummary.Foreground = summaryBrush;
                _serverSummaryText = $"{active}/{configured} sunucu";
                MiniServerText.Text = $"{active}/{configured} AKTİF";
                MiniServerText.Foreground = summaryBrush;
                MiniServerDot.Fill = summaryBrush;
            }

            UpdateTrayText();
            LastUpdateText.Text = "Son kontrol " + DateTime.Now.ToString("HH:mm:ss", Tr);

            var certificates = await Task.WhenAll(
                _settings.Endpoints.Select(e => CertificateService.GetAsync(e.Url, CancellationToken.None)));
            ApplyCertificates(certificates);
        }
        finally
        {
            _checking = false;
            RefreshButton.IsEnabled = true;
        }
    }

    private void ApplyCertificates(IReadOnlyList<CertificateInfo?> certificates)
    {
        CertificateInfo? nearest = null;
        var nearestName = "";

        for (int i = 0; i < _rows.Count && i < certificates.Count; i++)
        {
            var row = _rows[i];
            row.Cert = certificates[i];
            UpdateRowTooltip(row);

            if (certificates[i] is { } alertCert &&
                alertCert.DaysLeft <= 14 &&
                _sslAlerted.Add($"{row.Name}|{alertCert.NotAfter:yyyyMMdd}"))
            {
                NotificationLog.Add($"{row.Name} SSL", recovered: false);
                ShowBalloon(
                    "YEK Panel · SSL uyarısı",
                    alertCert.DaysLeft < 0
                        ? $"{row.Name}: sertifikanın süresi dolmuş."
                        : $"{row.Name}: sertifika {alertCert.DaysLeft} gün içinde doluyor.",
                    Forms.ToolTipIcon.Warning);
            }

            if (certificates[i] is { } info &&
                (nearest == null || info.DaysLeft < nearest.DaysLeft))
            {
                nearest = info;
                nearestName = row.Name;
            }
        }

        if (nearest is null)
        {
            SslSummary.Text = "SSL: veri yok";
            SslSummary.Foreground = Muted;
            SslSummary.ToolTip = "Sertifika bilgisi alınamadı.";
            return;
        }

        var days = nearest.DaysLeft;
        SslSummary.Text = days < 0
            ? $"SSL: süresi dolmuş · {nearestName}"
            : $"SSL: {days} gün · {nearestName}";
        SslSummary.Foreground = days < 0 || days <= 7
            ? StatusRed
            : days <= 21 ? StatusAmber : StatusGreen;
        SslSummary.ToolTip =
            $"{nearestName}: {nearest.NotAfter.ToString("dd.MM.yyyy", Tr)} ({days} gün)\n" +
            $"Veren: {nearest.Issuer}" +
            (nearest.SelfSigned ? "\nKendinden imzalı sertifika" : "");
    }

    private void ApplyResult(ServerRow row, ServerCheckResult result)
    {
        if (string.IsNullOrWhiteSpace(row.Url))
        {
            row.Dot.Fill = StatusGray;
            row.Status.Text = "ADRES YOK";
            row.Status.Foreground = Muted;
            row.Latency.Text = "—";
            row.Latency.Foreground = Muted;
            row.Wol.Visibility = Visibility.Collapsed;
            row.Detail = "Adres girilmedi";
            row.PortLine = "";
            row.RatioLine = "";
            row.LastState = 0;
            UpdateRowTooltip(row);
            return;
        }

        if (row.Maintenance)
        {
            row.Dot.Fill = StatusAmber;
            row.Status.Text = result.Responded ? "BAKIMDA" : "BAKIMDA · KAPALI";
            row.Status.Foreground = StatusAmber;
            row.Latency.Text = result.Responded && result.LatencyMs > 0
                ? $"{result.LatencyMs} ms"
                : "—";
            row.Latency.Foreground = Muted;
            row.Detail = $"Bakım modu etkin · bildirim üretilmez\n{row.Url}";
            row.PortLine = "";
            row.RatioLine = "Bakım modu";
            row.Wol.Visibility = Visibility.Collapsed;
            row.LastState = 0;

            row.History.Add(0);
            if (row.History.Count > ServerHistoryLength)
                row.History.RemoveAt(0);
            for (int i = 0; i < row.HistoryBars.Count; i++)
                row.HistoryBars[i].Background = StatusGray;

            UpdateRowTooltip(row);
            return;
        }

        string detail;
        int state;

        if (!result.Responded)
        {
            row.Dot.Fill = StatusRed;
            row.Status.Text = result.Error == "Zaman aşımı" ? "ZAMAN AŞIMI" : "ULAŞILAMIYOR";
            row.Status.Foreground = StatusRed;
            row.Latency.Text = result.LatencyMs > 0 ? $"{result.LatencyMs} ms" : "—";
            row.Latency.Foreground = result.LatencyMs > 0 ? StatusRed : Muted;
            detail = $"{row.Url}\n{result.Error}";
            state = 3;
        }
        else
        {
            var code = result.StatusCode ?? 0;
            var slow = result.LatencyMs > _settings.SlowThresholdMs;
            row.Latency.Text = $"{result.LatencyMs} ms";
            row.Latency.Foreground = result.LatencyMs <= 300 ? TextSoft : result.LatencyMs <= 800 ? StatusAmber : StatusRed;

            if (code >= 500)
            {
                row.Dot.Fill = StatusRed;
                row.Status.Text = $"HATA {code}";
                row.Status.Foreground = StatusRed;
                state = 3;
            }
            else if (code >= 400)
            {
                row.Dot.Fill = StatusAmber;
                row.Status.Text = "YANIT VERİYOR";
                row.Status.Foreground = StatusAmber;
                state = 2;
            }
            else if (slow)
            {
                row.Dot.Fill = StatusAmber;
                row.Status.Text = "YAVAŞ";
                row.Status.Foreground = StatusAmber;
                state = 1;
            }
            else
            {
                row.Dot.Fill = StatusGreen;
                row.Status.Text = "ÇEVRİMİÇİ";
                row.Status.Foreground = StatusGreen;
                state = 1;
            }

            detail = result.Note == null
                ? $"{row.Url} · HTTP {code}"
                : $"{row.Url} · HTTP {code}\n{result.Note}";

            if (slow && code < 400)
                detail += $"\nYavaş yanıt (eşik {_settings.SlowThresholdMs} ms)";
        }

        if (row.LastState == 1 && state >= 2)
        {
            NotificationLog.Add(row.Name, recovered: false);
            NotifyServer(row.Name, false);
        }
        else if (row.LastState >= 2 && state == 1)
        {
            NotificationLog.Add(row.Name, recovered: true);
            NotifyServer(row.Name, true);
        }
        row.LastState = state;

        row.Detail = detail;
        row.PortLine = result.PortChecked
            ? $"TCP {result.Port}: {(result.PortOpen ? "açık" : "kapalı")}"
            : "";
        row.Wol.Visibility = state == 3 && WakeOnLan.TryParseMac(row.Mac, out _)
            ? Visibility.Visible
            : Visibility.Collapsed;

        row.History.Add(state);
        if (row.History.Count > ServerHistoryLength)
            row.History.RemoveAt(0);

        for (int i = 0; i < row.HistoryBars.Count; i++)
        {
            var index = i - (ServerHistoryLength - row.History.Count);
            var value = index >= 0 ? row.History[index] : 0;
            row.HistoryBars[i].Background = value switch
            {
                1 => StatusGreen,
                2 => StatusAmber,
                3 => StatusRed,
                _ => StatusGray
            };
        }

        if (result.Responded && result.LatencyMs > 0)
        {
            row.Latencies.Add(result.LatencyMs);
            if (row.Latencies.Count > LatencyHistoryLength)
                row.Latencies.RemoveAt(0);
        }
        RenderLatency(row.LatencyChart, row.Latencies);

        UptimeHistory.Record(row.Name, state);
        var ratio = UptimeHistory.Percent(row.Name, TimeSpan.FromHours(24));
        row.RatioLine = ratio is double percent
            ? $"24s çevrimiçi: %{percent.ToString("0.0", Tr)}"
            : "24s: veri toplanıyor";

        UpdateRowTooltip(row);
    }

    private void UpdateRowTooltip(ServerRow row)
    {
        var lines = new List<string> { row.Detail };

        var online = row.History.Count(s => s == 1);
        if (row.History.Count > 0)
            lines.Add($"Son {row.History.Count} kontrol: {online} çevrimiçi");

        if (row.PortLine.Length > 0)
            lines.Add(row.PortLine);
        if (row.RatioLine.Length > 0)
            lines.Add(row.RatioLine);

        if (row.Cert is { } cert)
        {
            lines.Add(
                $"SSL: {cert.DaysLeft} gün (bitiş {cert.NotAfter.ToString("dd.MM.yyyy", Tr)}" +
                (cert.SelfSigned ? ", kendinden imzalı" : "") + ")");
        }
        else if (row.Url.StartsWith("https", StringComparison.OrdinalIgnoreCase))
        {
            lines.Add("SSL: okunamadı");
        }

        row.Root.ToolTip = string.Join("\n", lines);
    }

    private void RenderLatency(Canvas canvas, IReadOnlyList<long> values)
    {
        canvas.Children.Clear();

        var width = canvas.Width > 0 ? canvas.Width : 62;
        var height = canvas.Height > 0 ? canvas.Height : 14;

        if (values.Count == 0)
            return;

        var low = values.Min();
        var high = values.Max();
        var points = new PointCollection();

        for (int i = 0; i < values.Count; i++)
        {
            var x = values.Count == 1 ? width - 1 : width * i / (values.Count - 1);
            var y = high == low
                ? height / 2
                : height - 2 - (values[i] - low) / (double)(high - low) * (height - 4);
            points.Add(new Point(x, y));
        }

        if (values.Count == 1)
            points.Add(new Point(0, points[0].Y));

        canvas.Children.Add(new Polyline
        {
            Points = points,
            Stroke = _latencyLine,
            StrokeThickness = 1.2,
            StrokeLineJoin = PenLineJoin.Round
        });
    }

    private void NotifyServer(string name, bool recovered)
    {
        if (!_settings.NotificationsEnabled || IsQuietHours(DateTime.Now) ||
            _tray == null || !_tray.Visible || IsActive)
            return;

        try
        {
            _tray.ShowBalloonTip(
                6000,
                recovered ? "YEK Panel · Sunucu düzeldi" : "YEK Panel · Sunucu sorunu",
                recovered ? $"{name} tekrar yanıt veriyor." : $"{name} şu anda yanıt vermiyor.",
                recovered ? Forms.ToolTipIcon.Info : Forms.ToolTipIcon.Warning);
        }
        catch
        {
        }
    }

    private bool IsQuietHours(DateTime now)
    {
        if (!_settings.QuietHoursEnabled)
            return false;

        if (!TimeSpan.TryParse(_settings.QuietStart, CultureInfo.InvariantCulture, out var start) ||
            !TimeSpan.TryParse(_settings.QuietEnd, CultureInfo.InvariantCulture, out var end))
            return false;

        if (start == end)
            return false;

        var time = now.TimeOfDay;
        return start < end
            ? time >= start && time < end
            : time >= start || time < end;
    }

    private async Task UpdateInternetAsync()
    {
        var localIp = NetworkMonitor.GetLocalIp();
        var latency = await NetworkMonitor.PingAsync("1.1.1.1");

        if (latency is long value)
        {
            InternetText.Text = $"{value} ms";
            InternetText.Foreground = TextSoft;
            InternetDot.Fill = value <= 150 ? StatusGreen : value <= 400 ? StatusAmber : StatusRed;
            MiniPingText.Text = $"ping {value} ms";
            MiniPingText.Foreground = TextSoft;
        }
        else
        {
            InternetText.Text = "yok";
            InternetText.Foreground = StatusRed;
            InternetDot.Fill = StatusRed;
            MiniPingText.Text = "ping yok";
            MiniPingText.Foreground = StatusRed;
        }

        InternetBox.ToolTip = $"Yerel IP: {localIp}\nDış IP: {_publicIp}\nHedef: 1.1.1.1";

        if (_publicIp == "—" || DateTime.Now - _lastPublicIpCheck > TimeSpan.FromMinutes(10))
            _ = RefreshPublicIpAsync();
    }

    private async Task RefreshWeatherAsync()
    {
        if (_weatherLoading)
            return;

        _weatherLoading = true;
        try
        {
            var info = await WeatherService.FetchAsync(_http, _settings.City, CancellationToken.None);
            _weather = info;

            if (info == null)
            {
                WeatherGlyph.Text = "☁";
                WeatherTemp.Text = "—";
                WeatherDesc.Text = "hava alınamadı";
                WeatherCity.Text = "";
                WeatherForecast.Text = "";
                MiniWeatherGlyph.Text = "☁";
                MiniWeatherTemp.Text = "—";
                MiniWeatherCity.Text = "";
                WeatherChip.ToolTip = "Hava durumu alınamadı. Ayarlardaki şehir bilgisini kontrol edin.";
                UpdateWeatherDetail();
                return;
            }

            WeatherGlyph.Text = info.Glyph;
            WeatherTemp.Text = _settings.UseFahrenheit ? TempWithUnit(info.Temp) : TempNumber(info.Temp);
            WeatherDesc.Text = info.Description;
            WeatherCity.Text = info.City;
            WeatherForecast.Text =
                $"Yarın {TempNumber(info.TomorrowMin)}/{TempNumber(info.TomorrowMax)}" +
                (_settings.UseFahrenheit ? "F" : "");
            MiniWeatherGlyph.Text = info.Glyph;
            MiniWeatherTemp.Text = WeatherTemp.Text;
            MiniWeatherCity.Text = $"{info.City} · {info.Description}";
            UpdateWeatherDetail();
            WeatherChip.ToolTip =
                $"{info.City} · hissedilen {TempWithUnit(info.FeelsLike)}\n" +
                $"Nem %{info.Humidity} · Rüzgar {info.Wind.ToString("0", Tr)} km/s\n" +
                $"Bugün {TempNumber(info.Min)}/{TempNumber(info.Max)}\n" +
                $"Yarın {info.TomorrowDescription} {TempNumber(info.TomorrowMin)}/{TempNumber(info.TomorrowMax)}\n" +
                $"Öbür gün {info.DayAfterDescription} {TempNumber(info.DayAfterMin)}/{TempNumber(info.DayAfterMax)}";
        }
        finally
        {
            _weatherLoading = false;
        }
    }

    private void UpdateWeatherDetail()
    {
        if (_weather is not { } info)
        {
            WeatherDetailCity.Text = "veri yok";
            WeatherDetailGlyph.Text = "☁";
            WeatherDetailTemp.Text = "—";
            WeatherDetailDesc.Text = "Hava durumu alınamadı";
            WeatherDetailFeels.Text = "—";
            WeatherDetailHumidity.Text = "—";
            WeatherDetailWind.Text = "—";
            WeatherDetailToday.Text = "—";
            WeatherDetailTomorrow.Text = "—";
            WeatherDetailDayAfter.Text = "—";
            return;
        }

        WeatherDetailCity.Text = info.City;
        WeatherDetailGlyph.Text = info.Glyph;
        WeatherDetailTemp.Text = WeatherTemp.Text;
        WeatherDetailDesc.Text = info.Description;
        WeatherDetailFeels.Text = TempWithUnit(info.FeelsLike);
        WeatherDetailHumidity.Text = $"%{info.Humidity}";
        WeatherDetailWind.Text = $"{info.Wind.ToString("0", Tr)} km/s";
        WeatherDetailToday.Text = $"{TempNumber(info.Min)} / {TempNumber(info.Max)}";
        WeatherDetailTomorrow.Text =
            $"{info.TomorrowDescription} · {TempNumber(info.TomorrowMin)} / {TempNumber(info.TomorrowMax)}";
        WeatherDetailDayAfter.Text =
            $"{info.DayAfterDescription} · {TempNumber(info.DayAfterMin)} / {TempNumber(info.DayAfterMax)}";
    }

    private void WeatherChip_Click(object sender, RoutedEventArgs e)
    {
        SettingsOverlay.Visibility = Visibility.Collapsed;
        UpdateWeatherDetail();
        WeatherOverlay.Visibility = Visibility.Visible;
    }

    private void WeatherClose_Click(object sender, RoutedEventArgs e) =>
        WeatherOverlay.Visibility = Visibility.Collapsed;

    private void UpdatePinVisual() => PinButton.Foreground = Topmost ? _accent : Muted;

    private void InitializeTray()
    {
        try
        {
            var menu = new Forms.ContextMenuStrip();
            menu.Items.Add("Göster", null, (_, _) => ShowFromTray());
            menu.Items.Add("Yenile", null, (_, _) => _ = CheckServersAsync());
            menu.Items.Add("Ayarlar", null, (_, _) =>
            {
                ShowFromTray();
                Dispatcher.BeginInvoke(new Action(OpenSettings));
            });
            menu.Items.Add("Geçmiş ve rapor", null, (_, _) =>
            {
                ShowFromTray();
                Dispatcher.BeginInvoke(new Action(() => OpenHistory_Click(this, new RoutedEventArgs())));
            });
            menu.Items.Add("Araç merkezi", null, (_, _) =>
            {
                ShowFromTray();
                Dispatcher.BeginInvoke(new Action(() => OpenTools_Click(this, new RoutedEventArgs())));
            });
            menu.Items.Add("Mini mod", null, (_, _) => Dispatcher.BeginInvoke(new Action(ToggleMini)));
            menu.Items.Add("Hız testi", null, (_, _) =>
            {
                ShowFromTray();
                Dispatcher.BeginInvoke(new Action(() => SpeedTest_Click(this, new RoutedEventArgs())));
            });

            var quietItem = new Forms.ToolStripMenuItem("Sessiz saatler")
            {
                CheckOnClick = true,
                Checked = _settings.QuietHoursEnabled
            };
            quietItem.Click += (_, _) =>
            {
                _settings.QuietHoursEnabled = quietItem.Checked;
                SettingsStore.Save(_settings);
            };
            menu.Items.Add(quietItem);

            menu.Items.Add("Yönetici olarak yeniden başlat", null, (_, _) =>
                Dispatcher.BeginInvoke(new Action(RestartAsAdmin)));
            menu.Items.Add(new Forms.ToolStripSeparator());
            menu.Items.Add("Çıkış", null, (_, _) => Dispatcher.BeginInvoke(new Action(Close)));

            var icon = Environment.ProcessPath is { Length: > 0 } path
                ? Drawing.Icon.ExtractAssociatedIcon(path)
                : null;

            _tray = new Forms.NotifyIcon
            {
                Icon = icon ?? Drawing.SystemIcons.Application,
                Text = "YEK Panel",
                Visible = true,
                ContextMenuStrip = menu
            };
            _tray.DoubleClick += (_, _) => ShowFromTray();
        }
        catch
        {
            _tray = null;
        }
    }

    private void ShowFromTray()
    {
        Show();
        WindowState = WindowState.Normal;
        Activate();
    }

    protected override void OnClosed(EventArgs e)
    {
        SaveWindowPosition();
        _clockTimer.Stop();
        _serverTimer.Stop();
        _weatherTimer.Stop();
        _netTimer.Stop();
        _processTimer.Stop();
        _temperatureTimer.Stop();
        _stopwatchTimer.Stop();
        _countdownTimer.Stop();
        _summaryTimer.Stop();
        _connectionTimer.Stop();
        _stopwatch.Stop();
        _sensors.Dispose();

        if (_tray != null)
        {
            _tray.Visible = false;
            _tray.Dispose();
            _tray = null;
        }

        base.OnClosed(e);
    }

    private void Pin_Click(object sender, RoutedEventArgs e)
    {
        Topmost = !Topmost;
        _settings.Topmost = Topmost;
        SettingsStore.Save(_settings);
        UpdatePinVisual();
    }

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.LeftButton == MouseButtonState.Pressed)
            DragMove();
    }

    private void Window_KeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Escape)
        {
            if (ToolsOverlay.Visibility == Visibility.Visible)
                ToolsOverlay.Visibility = Visibility.Collapsed;
            else if (SettingsOverlay.Visibility == Visibility.Visible)
                SettingsOverlay.Visibility = Visibility.Collapsed;
            else if (WeatherOverlay.Visibility == Visibility.Visible)
                WeatherOverlay.Visibility = Visibility.Collapsed;
            else if (HistoryOverlay.Visibility == Visibility.Visible)
                HistoryOverlay.Visibility = Visibility.Collapsed;
            else
                return;

            e.Handled = true;
            return;
        }

        var ctrl = (Keyboard.Modifiers & ModifierKeys.Control) == ModifierKeys.Control;

        if (ctrl && e.Key == Key.K)
        {
            ToggleTools();
            e.Handled = true;
            return;
        }

        if (SettingsOverlay.Visibility == Visibility.Visible)
            return;

        if (e.Key == Key.F5 || (ctrl && e.Key == Key.R))
        {
            _ = CheckServersAsync();
            _ = UpdateInternetAsync();
            UpdateSystem();
            e.Handled = true;
        }
        else if (ctrl && e.Key == Key.OemComma)
        {
            OpenSettings();
            e.Handled = true;
        }
        else if (ctrl && e.Key == Key.T)
        {
            Pin_Click(this, new RoutedEventArgs());
            e.Handled = true;
        }
        else if (ctrl && e.Key == Key.M)
        {
            ToggleMini();
            e.Handled = true;
        }
        else if (ctrl && e.Key == Key.H)
        {
            OpenHistory_Click(this, new RoutedEventArgs());
            e.Handled = true;
        }
    }

    private void Minimize_Click(object sender, RoutedEventArgs e) => WindowState = WindowState.Minimized;

    private void Mini_Click(object sender, RoutedEventArgs e) => ToggleMini();

    private void ToggleMini()
    {
        _miniMode = !_miniMode;

        if (_miniMode)
        {
            SettingsOverlay.Visibility = Visibility.Collapsed;
            WeatherOverlay.Visibility = Visibility.Collapsed;
            HistoryOverlay.Visibility = Visibility.Collapsed;
            ToolsOverlay.Visibility = Visibility.Collapsed;
            BrandRow.Visibility = Visibility.Collapsed;
            ContentScroller.Visibility = Visibility.Collapsed;
            FooterRow.Visibility = Visibility.Collapsed;
            MiniPanel.Visibility = Visibility.Visible;
            Height = MiniHeight;
            MiniButton.Content = "\uE73F";
            MiniButton.ToolTip = "Tam görünüm (Ctrl+M)";
            System.Windows.Automation.AutomationProperties.SetName(MiniButton, "Tam görünüm");
        }
        else
        {
            MiniPanel.Visibility = Visibility.Collapsed;
            BrandRow.Visibility = Visibility.Visible;
            ContentScroller.Visibility = Visibility.Visible;
            FooterRow.Visibility = Visibility.Visible;
            Height = FullHeight;
            MiniButton.Content = "\uE740";
            MiniButton.ToolTip = "Mini mod (Ctrl+M)";
            System.Windows.Automation.AutomationProperties.SetName(MiniButton, "Mini mod");
        }

        EnsureOnScreen();
        _settings.MiniMode = _miniMode;
        SaveWindowPosition();
    }

    private void EnsureOnScreen()
    {
        var work = SystemParameters.WorkArea;

        if (Width > work.Width)
            Width = work.Width;
        if (Height > work.Height)
            Height = work.Height;

        if (Left + Width > work.Right)
            Left = work.Right - Width;
        if (Top + Height > work.Bottom)
            Top = work.Bottom - Height;
        if (Left < work.Left)
            Left = work.Left;
        if (Top < work.Top)
            Top = work.Top;
    }

    private void SaveWindowPosition()
    {
        if (WindowState != WindowState.Normal)
            return;

        _settings.WindowLeft = Left;
        _settings.WindowTop = Top;
        SettingsStore.Save(_settings);
    }

    private void Close_Click(object sender, RoutedEventArgs e)
    {
        if (_settings.CloseToTray && _tray != null)
        {
            SaveWindowPosition();
            Hide();
            ShowTrayHint();
            return;
        }

        Close();
    }

    private void ShowTrayHint()
    {
        if (_trayHintShown)
            return;

        _trayHintShown = true;
        if (_tray == null)
            return;

        try
        {
            _tray.ShowBalloonTip(
                6000,
                "YEK Panel",
                "Tepside çalışmaya devam ediyor. Açmak için simgeye çift tıklayın.",
                Forms.ToolTipIcon.Info);
        }
        catch
        {
        }
    }

    private async void Refresh_Click(object sender, RoutedEventArgs e) => await CheckServersAsync();

    private async void SpeedTest_Click(object sender, RoutedEventArgs e)
    {
        if (_speedTesting)
            return;

        _speedTesting = true;
        SpeedTestButton.IsEnabled = false;
        SpeedTestValue.Text = "ölçülüyor…";
        SpeedTestValue.Foreground = Muted;

        try
        {
            var mbps = await SpeedTestService.MeasureDownloadAsync(CancellationToken.None);
            if (mbps is double value)
            {
                SpeedTestValue.Text = $"{value.ToString("0.0", Tr)} Mbps";
                SpeedTestValue.Foreground = value >= 25 ? StatusGreen : value >= 8 ? StatusAmber : StatusRed;
                SpeedTestValue.ToolTip = $"İndirme testi · {DateTime.Now.ToString("HH:mm", Tr)}";
            }
            else
            {
                SpeedTestValue.Text = "başarısız";
                SpeedTestValue.Foreground = StatusRed;
                SpeedTestValue.ToolTip = "Hız testi tamamlanamadı.";
            }
        }
        finally
        {
            _speedTesting = false;
            SpeedTestButton.IsEnabled = true;
        }
    }

    private async Task UpdateTemperaturesAsync()
    {
        var sample = await Task.Run(_sensors.Read);

        if (sample.Cpu is null && sample.Gpu is null)
        {
            TempValue.Text = "—";
            TempValue.Foreground = Muted;
            TempValue.ToolTip = sample.Note ?? "Sıcaklık sensörü okunamadı.";
            return;
        }

        var parts = new List<string>();
        double max = 0;

        if (sample.Cpu is double cpu)
        {
            parts.Add($"CPU {TempWithUnit(cpu)}");
            max = Math.Max(max, cpu);
        }

        if (sample.Gpu is double gpu)
        {
            parts.Add($"GPU {TempWithUnit(gpu)}");
            max = Math.Max(max, gpu);
        }

        TempValue.Text = string.Join(" · ", parts);
        TempValue.Foreground = max >= 85 ? StatusRed : max >= 70 ? StatusAmber : TextSoft;
        TempValue.ToolTip = "Sıcaklıklar 8 saniyede bir okunur." +
                            (sample.Note == null ? "" : "\n" + sample.Note);
    }

    private async Task WakeServerAsync(string name, string mac)
    {
        if (!WakeOnLan.TryParseMac(mac, out _))
            return;

        var sent = WakeOnLan.Send(mac);
        LastUpdateText.Text = sent
            ? $"{name}: açma sinyali gönderildi"
            : $"{name}: açma sinyali gönderilemedi";

        if (_tray is { Visible: true })
        {
            try
            {
                _tray.ShowBalloonTip(
                    5000,
                    "YEK Panel · Wake-on-LAN",
                    sent
                        ? $"{name} için açma sinyali gönderildi ({WakeOnLan.Normalize(mac)})."
                        : $"{name} için açma sinyali gönderilemedi.",
                    Forms.ToolTipIcon.Info);
            }
            catch
            {
            }
        }

        if (sent)
        {
            await Task.Delay(8000);
            await CheckServersAsync();
        }
    }

    private void OpenHistory_Click(object sender, RoutedEventArgs e)
    {
        if (_miniMode)
            ToggleMini();

        SettingsOverlay.Visibility = Visibility.Collapsed;
        WeatherOverlay.Visibility = Visibility.Collapsed;
        BuildHistoryReport();
        HistoryOverlay.Visibility = Visibility.Visible;
    }

    private void HistoryClose_Click(object sender, RoutedEventArgs e) =>
        HistoryOverlay.Visibility = Visibility.Collapsed;

    private void BuildHistoryReport()
    {
        ReportList.Children.Clear();
        NotificationList.Children.Clear();
        IncidentList.Children.Clear();
        ReportStatus.Text = "";

        var range = _reportDays;
        HistoryRangeText.Text = $"Son {range} günün çevrimiçi oranı, kesintiler ve bildirim kayıtları.";
        ReportRangeLabel.Text = $"{range} GÜNLÜK ÇEVRİMİÇİ";
        Report7Text.Foreground = range == 7 ? _accentText : Muted;
        Report30Text.Foreground = range == 30 ? _accentText : Muted;

        var configured = _settings.Endpoints
            .Where(e => !string.IsNullOrWhiteSpace(e.Url))
            .ToList();

        if (configured.Count == 0)
        {
            ReportList.Children.Add(new TextBlock
            {
                Text = "Sunucu adresi tanımlı değil.",
                FontSize = 10,
                Foreground = Muted
            });
        }

        foreach (var endpoint in configured)
        {
            var days = UptimeHistory.Daily(endpoint.Name, range);
            var known = days.Where(d => d.Percent.HasValue).Select(d => d.Percent!.Value).ToList();
            double? average = known.Count > 0 ? known.Average() : null;

            var block = new StackPanel { Margin = new Thickness(0, 0, 0, 10) };
            var header = new Grid();
            header.Children.Add(new TextBlock
            {
                Text = endpoint.Name,
                FontSize = 11,
                FontWeight = FontWeights.SemiBold,
                Foreground = TextSoft,
                TextTrimming = TextTrimming.CharacterEllipsis
            });
            header.Children.Add(new TextBlock
            {
                Text = average is double value ? $"%{value.ToString("0.0", Tr)}" : "veri yok",
                FontFamily = new FontFamily("Consolas"),
                FontSize = 10.5,
                Foreground = PercentBrush(average),
                HorizontalAlignment = HorizontalAlignment.Right
            });
            block.Children.Add(header);

            var chart = new Grid { Margin = new Thickness(0, 6, 0, 0) };
            for (int i = 0; i < range; i++)
                chart.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            var barWidth = range > 7 ? 8d : 16d;
            var barHeight = range > 7 ? 22d : 30d;

            for (int i = 0; i < days.Count; i++)
            {
                var day = days[i];
                var cell = new StackPanel { HorizontalAlignment = HorizontalAlignment.Center };

                var track = new Grid
                {
                    Height = barHeight,
                    Width = barWidth,
                    VerticalAlignment = VerticalAlignment.Bottom
                };
                track.Children.Add(new Border
                {
                    Background = MakeBrush(0x0A, 0x1E, 0x38),
                    CornerRadius = new CornerRadius(3)
                });

                var percent = day.Percent ?? 0;
                track.Children.Add(new Border
                {
                    Height = Math.Max(2, percent / 100 * barHeight),
                    Background = PercentBrush(day.Percent),
                    CornerRadius = new CornerRadius(3),
                    VerticalAlignment = VerticalAlignment.Bottom,
                    ToolTip = day.Percent is double p
                        ? $"{day.Day.ToString("dd.MM.yyyy", Tr)}: %{p.ToString("0.0", Tr)}"
                        : $"{day.Day.ToString("dd.MM.yyyy", Tr)}: veri yok"
                });

                cell.Children.Add(track);

                var label = range > 7
                    ? i == days.Count - 1 || i % 5 == 4 ? day.Day.ToString("dd", Tr) : ""
                    : day.Day.ToString("ddd", Tr);

                cell.Children.Add(new TextBlock
                {
                    Text = label,
                    FontSize = 8.5,
                    Foreground = Muted,
                    HorizontalAlignment = HorizontalAlignment.Center,
                    Margin = new Thickness(0, 3, 0, 0)
                });

                Grid.SetColumn(cell, i);
                chart.Children.Add(cell);
            }

            block.Children.Add(chart);
            ReportList.Children.Add(block);
        }

        var incidents = UptimeHistory.Incidents(range, 10);
        if (incidents.Count == 0)
        {
            IncidentList.Children.Add(new TextBlock
            {
                Text = "Bu aralıkta kesinti kaydı yok.",
                FontSize = 10,
                Foreground = StatusGreen
            });
        }

        foreach (var incident in incidents)
        {
            var line = new Grid { Margin = new Thickness(0, 0, 0, 4) };
            line.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            line.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

            line.Children.Add(new TextBlock
            {
                Text = $"{incident.StartLocal.ToString("dd.MM HH:mm", Tr)} · {incident.Server}",
                FontSize = 10,
                Foreground = TextSoft,
                TextTrimming = TextTrimming.CharacterEllipsis
            });

            var duration = new TextBlock
            {
                Text = incident.Ongoing ? "sürüyor" : FormatDuration(incident.Duration),
                FontFamily = new FontFamily("Consolas"),
                FontSize = 10,
                Foreground = incident.Ongoing ? StatusAmber : StatusRed,
                HorizontalAlignment = HorizontalAlignment.Right,
                Margin = new Thickness(8, 0, 0, 0)
            };
            Grid.SetColumn(duration, 1);
            line.Children.Add(duration);
            IncidentList.Children.Add(line);
        }

        var recent = NotificationLog.Recent(8);
        if (recent.Count == 0)
        {
            NotificationList.Children.Add(new TextBlock
            {
                Text = "Henüz bildirim kaydı yok.",
                FontSize = 10,
                Foreground = Muted
            });
        }

        foreach (var entry in recent)
        {
            NotificationList.Children.Add(new TextBlock
            {
                Text = $"{entry.Utc.ToLocalTime().ToString("dd.MM HH:mm", Tr)} · {entry.Server} · " +
                       (entry.Recovered ? "düzeldi" : "sorun"),
                FontSize = 10,
                Foreground = entry.Recovered ? StatusGreen : StatusRed,
                Margin = new Thickness(0, 0, 0, 4),
                TextTrimming = TextTrimming.CharacterEllipsis
            });
        }
    }

    private static string FormatDuration(TimeSpan span)
    {
        if (span.TotalHours >= 1)
            return $"{(int)span.TotalHours} sa {span.Minutes} dk";
        if (span.TotalMinutes >= 1)
            return $"{Math.Max(1, (int)Math.Round(span.TotalMinutes))} dk";
        return $"{Math.Max(1, (int)Math.Round(span.TotalSeconds))} sn";
    }

    private static Brush PercentBrush(double? percent) => percent switch
    {
        >= 99 => StatusGreen,
        >= 90 => StatusAmber,
        null => StatusGray,
        _ => StatusRed
    };

    private void ExportReport_Click(object sender, RoutedEventArgs e)
    {
        var path = ReportExporter.Export(
            _settings, _lastCpuPercent, _lastRamPercent, _lastDiskPercent, _reportDays);
        if (path is null)
        {
            ReportStatus.Text = "Rapor kaydedilemedi.";
            ReportStatus.Foreground = StatusRed;
            return;
        }

        ReportStatus.Text = $"Kaydedildi ({_reportDays} gün): " + path;
        ReportStatus.ToolTip = path;
        ReportStatus.Foreground = StatusGreen;
    }

    private void RestartAsAdmin()
    {
        try
        {
            var path = Environment.ProcessPath;
            if (string.IsNullOrEmpty(path))
                return;

            Process.Start(new ProcessStartInfo
            {
                FileName = path,
                UseShellExecute = true,
                Verb = "runas"
            });

            Close();
        }
        catch
        {
            // Kullanıcı UAC isteğini iptal etmiş olabilir.
        }
    }

    private void OpenSettings_Click(object sender, RoutedEventArgs e) => OpenSettings();

    private void OpenSettings()
    {
        if (_miniMode)
            ToggleMini();

        UrlSite.Text = UrlAt(0);
        UrlPanel.Text = UrlAt(1);
        UrlHa.Text = UrlAt(2);
        UrlCloud.Text = UrlAt(3);
        MacSite.Text = MacAt(0);
        MacPanel.Text = MacAt(1);
        MacHa.Text = MacAt(2);
        MacCloud.Text = MacAt(3);
        CityBox.Text = _settings.City;
        RefreshBox.Text = _settings.RefreshSeconds.ToString(Tr);
        TopmostCheck.IsChecked = _settings.Topmost;
        StartupCheck.IsChecked = _settings.StartWithWindows;
        NotificationsCheck.IsChecked = _settings.NotificationsEnabled;
        QuietCheck.IsChecked = _settings.QuietHoursEnabled;
        QuietStartBox.Text = _settings.QuietStart;
        QuietEndBox.Text = _settings.QuietEnd;
        CloseToTrayCheck.IsChecked = _settings.CloseToTray;
        Clock24Check.IsChecked = _settings.Clock24Hour;
        SecondsCheck.IsChecked = _settings.ShowSeconds;
        FahrenheitCheck.IsChecked = _settings.UseFahrenheit;
        StartHiddenCheck.IsChecked = _settings.StartHidden;
        RememberMiniCheck.IsChecked = _settings.RememberMini;
        SlowThresholdBox.Text = _settings.SlowThresholdMs.ToString(Tr);
        DailySummaryCheck.IsChecked = _settings.DailySummaryEnabled;
        SummaryTimeBox.Text = _settings.DailySummaryTime;
        MaintenanceSite.IsChecked = _settings.Endpoints[0].Maintenance;
        MaintenancePanel.IsChecked = _settings.Endpoints[1].Maintenance;
        MaintenanceHa.IsChecked = _settings.Endpoints[2].Maintenance;
        MaintenanceCloud.IsChecked = _settings.Endpoints[3].Maintenance;
        _pendingOpacity = _settings.WindowOpacity;
        OpacityValue.Text = $"%{(_pendingOpacity * 100).ToString("0", Tr)}";
        _selectedAccent = _settings.Accent;
        UpdateAccentSwatches();
        HistoryOverlay.Visibility = Visibility.Collapsed;
        WeatherOverlay.Visibility = Visibility.Collapsed;
        ToolsOverlay.Visibility = Visibility.Collapsed;
        SettingsOverlay.Visibility = Visibility.Visible;
    }

    private string UrlAt(int index) =>
        _settings.Endpoints.Count > index ? _settings.Endpoints[index].Url : "";

    private string MacAt(int index) =>
        _settings.Endpoints.Count > index ? _settings.Endpoints[index].Mac : "";

    private void SaveSettings_Click(object sender, RoutedEventArgs e)
    {
        _settings.Endpoints[0].Url = UrlSite.Text.Trim();
        _settings.Endpoints[1].Url = UrlPanel.Text.Trim();
        _settings.Endpoints[2].Url = UrlHa.Text.Trim();
        _settings.Endpoints[3].Url = UrlCloud.Text.Trim();
        _settings.Endpoints[0].Mac = WakeOnLan.Normalize(MacSite.Text);
        _settings.Endpoints[1].Mac = WakeOnLan.Normalize(MacPanel.Text);
        _settings.Endpoints[2].Mac = WakeOnLan.Normalize(MacHa.Text);
        _settings.Endpoints[3].Mac = WakeOnLan.Normalize(MacCloud.Text);
        _settings.Endpoints[0].Maintenance = MaintenanceSite.IsChecked == true;
        _settings.Endpoints[1].Maintenance = MaintenancePanel.IsChecked == true;
        _settings.Endpoints[2].Maintenance = MaintenanceHa.IsChecked == true;
        _settings.Endpoints[3].Maintenance = MaintenanceCloud.IsChecked == true;

        if (int.TryParse(RefreshBox.Text.Trim(), NumberStyles.Integer, Tr, out var seconds))
            _settings.RefreshSeconds = Math.Clamp(seconds, 5, 3600);

        if (int.TryParse(SlowThresholdBox.Text.Trim(), NumberStyles.Integer, Tr, out var slowMs))
            _settings.SlowThresholdMs = Math.Clamp(slowMs, 100, 30000);

        _settings.Topmost = TopmostCheck.IsChecked == true;
        _settings.StartWithWindows = StartupCheck.IsChecked == true;
        _settings.NotificationsEnabled = NotificationsCheck.IsChecked == true;
        _settings.QuietHoursEnabled = QuietCheck.IsChecked == true;
        _settings.QuietStart = NormalizeClock(QuietStartBox.Text, _settings.QuietStart);
        _settings.QuietEnd = NormalizeClock(QuietEndBox.Text, _settings.QuietEnd);
        _settings.CloseToTray = CloseToTrayCheck.IsChecked == true;
        _settings.Clock24Hour = Clock24Check.IsChecked == true;
        _settings.ShowSeconds = SecondsCheck.IsChecked == true;
        _settings.UseFahrenheit = FahrenheitCheck.IsChecked == true;
        _settings.StartHidden = StartHiddenCheck.IsChecked == true;
        _settings.RememberMini = RememberMiniCheck.IsChecked == true;
        _settings.MiniMode = _miniMode;
        _settings.DailySummaryEnabled = DailySummaryCheck.IsChecked == true;
        _settings.DailySummaryTime = NormalizeClock(SummaryTimeBox.Text, _settings.DailySummaryTime);
        _settings.WindowOpacity = _pendingOpacity;
        _settings.City = CityBox.Text.Trim();
        _settings.Accent = _selectedAccent;

        SettingsStore.Save(_settings);
        ApplyStartupSetting();
        ApplyAccent(_settings.Accent, persist: false);
        Opacity = _settings.WindowOpacity;

        Topmost = _settings.Topmost;
        UpdatePinVisual();
        _serverTimer.Interval = TimeSpan.FromSeconds(_settings.RefreshSeconds);
        UpdateClock();

        BuildServerRows();
        ToolsOverlay.Visibility = Visibility.Collapsed;
        SettingsOverlay.Visibility = Visibility.Collapsed;
        _ = CheckServersAsync();
        _ = RefreshWeatherAsync();
        _ = UpdateTemperaturesAsync();
    }

    private static string NormalizeClock(string? text, string fallback) =>
        TimeSpan.TryParseExact((text ?? "").Trim(), @"hh\:mm", CultureInfo.InvariantCulture, out var time)
            ? time.ToString(@"hh\:mm")
            : fallback;

    private void CancelSettings_Click(object sender, RoutedEventArgs e)
    {
        ApplyAccent(_settings.Accent, persist: false);
        _pendingOpacity = _settings.WindowOpacity;
        Opacity = _settings.WindowOpacity;
        SettingsOverlay.Visibility = Visibility.Collapsed;
    }

    private void Accent_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button { Tag: string key })
        {
            ApplyAccent(key, persist: false);
        }
    }

    private void ApplyStartupSetting()
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(
                @"Software\Microsoft\Windows\CurrentVersion\Run", writable: true);
            if (key == null)
                return;

            if (_settings.StartWithWindows)
            {
                var path = Environment.ProcessPath;
                if (!string.IsNullOrEmpty(path))
                    key.SetValue("YekPanel", $"\"{path}\"");
            }
            else
            {
                key.DeleteValue("YekPanel", false);
            }
        }
        catch
        {
            // Kayıt defterine erişilemezse diğer ayarlar yine de geçerli olur.
        }
    }

    private void ToggleTools()
    {
        if (ToolsOverlay.Visibility == Visibility.Visible)
        {
            ToolsOverlay.Visibility = Visibility.Collapsed;
            return;
        }

        OpenTools_Click(this, new RoutedEventArgs());
    }

    private void OpenTools_Click(object sender, RoutedEventArgs e)
    {
        if (_miniMode)
            ToggleMini();

        SettingsOverlay.Visibility = Visibility.Collapsed;
        WeatherOverlay.Visibility = Visibility.Collapsed;
        HistoryOverlay.Visibility = Visibility.Collapsed;

        NoteBox.Text = _settings.QuickNote;
        ToolPublicIpText.Text = _publicIp;

        if (ToolsHost.Text.Trim().Length == 0 &&
            Uri.TryCreate(UrlAt(0), UriKind.Absolute, out var first) &&
            !string.IsNullOrWhiteSpace(first.Host))
        {
            ToolsHost.Text = first.Host;
        }

        if (ToolsPortHost.Text.Trim().Length == 0)
            ToolsPortHost.Text = ToolsHost.Text;

        ToolsOverlay.Visibility = Visibility.Visible;
        SetToolsStatus("bilgiler güncelleniyor…", Muted);
        _ = RefreshToolsAsync();
    }

    private void ToolsClose_Click(object sender, RoutedEventArgs e) =>
        ToolsOverlay.Visibility = Visibility.Collapsed;

    private async void RefreshTools_Click(object sender, RoutedEventArgs e) =>
        await RefreshToolsAsync();

    private async void RefreshConnection_Click(object sender, RoutedEventArgs e) =>
        await RefreshConnectionAsync(force: true);

    private async Task RefreshToolsAsync() => await RefreshConnectionAsync(force: true);

    private async Task RefreshConnectionAsync(bool force = false)
    {
        if (_connectionLoading)
            return;

        if (!force && DateTime.Now - _lastConnectionRefresh < TimeSpan.FromSeconds(5))
            return;

        _connectionLoading = true;
        MainConnStatus.Text = "güncelleniyor…";
        MainConnStatus.Foreground = Muted;

        try
        {
            var (connection, drives, battery) = await Task.Run(() =>
                (ToolsService.GetConnection(), ToolsService.GetDrives(), ToolsService.GetBattery()));

            _lastConnectionRefresh = DateTime.Now;
            ApplyConnection(connection, drives, battery);
            DetectLocalIpChange(connection.Ipv4);

            if (DateTime.Now - _lastPublicIpCheck > TimeSpan.FromMinutes(10))
                _ = RefreshPublicIpAsync();

            MainConnStatus.Text = $"güncellendi · {DateTime.Now.ToString("HH:mm", Tr)}";
            MainConnStatus.Foreground = StatusGreen;

            if (ToolsOverlay.Visibility == Visibility.Visible)
                SetToolsStatus($"güncellendi · {DateTime.Now.ToString("HH:mm:ss", Tr)}", StatusGreen);
        }
        catch
        {
            MainConnStatus.Text = "bilgi alınamadı";
            MainConnStatus.Foreground = StatusRed;
            if (ToolsOverlay.Visibility == Visibility.Visible)
                SetToolsStatus("araç bilgileri alınamadı", StatusRed);
        }
        finally
        {
            _connectionLoading = false;
        }
    }

    private void ApplyConnection(ConnectionSummary connection, IReadOnlyList<DriveUsage> drives, BatteryInfo battery)
    {
        var adapter = string.IsNullOrWhiteSpace(connection.Speed)
            ? connection.Adapter
            : $"{connection.Adapter} · {connection.Speed}";

        MainBatteryText.Text = battery.Text;
        MainBatteryDetail.Text = battery.Detail;
        MainAdapterText.Text = adapter;
        MainIpText.Text = connection.Ipv4;
        MainGatewayText.Text = connection.Gateway;
        MainDnsText.Text = connection.Dns;
        MainWifiText.Text = connection.Wifi;
        MainPublicIpText.Text = _publicIp;

        FillDriveList(MainDriveList, drives, compact: true);

        ToolBatteryText.Text = battery.Text;
        ToolBatteryDetail.Text = battery.Detail;
        ToolAdapterText.Text = adapter;
        ToolIpText.Text = connection.Ipv4;
        ToolGatewayText.Text = connection.Gateway;
        ToolDnsText.Text = connection.Dns;
        ToolWifiText.Text = connection.Wifi;
        ToolPublicIpText.Text = _publicIp;

        FillDriveList(DriveList, drives, compact: false);
    }

    private void FillDriveList(Panel target, IReadOnlyList<DriveUsage> drives, bool compact)
    {
        target.Children.Clear();

        if (drives.Count == 0)
        {
            target.Children.Add(new TextBlock
            {
                Text = "disk bulunamadı",
                FontSize = compact ? 9.5 : 10,
                Foreground = Muted
            });
            return;
        }

        foreach (var drive in drives)
        {
            var panel = new StackPanel { Margin = new Thickness(0, 0, 0, compact ? 6 : 8) };

            var line = new Grid();
            line.Children.Add(new TextBlock
            {
                Text = drive.Name,
                FontSize = compact ? 10 : 10.5,
                FontWeight = FontWeights.SemiBold,
                Foreground = TextSoft
            });
            line.Children.Add(new TextBlock
            {
                Text = $"%{drive.Percent.ToString("0", Tr)} · " +
                       $"{drive.UsedGb.ToString("0.0", Tr)}/{drive.TotalGb.ToString("0.0", Tr)} GB",
                FontSize = compact ? 9 : 9.5,
                Foreground = Muted,
                HorizontalAlignment = HorizontalAlignment.Right
            });

            panel.Children.Add(line);
            panel.Children.Add(new ProgressBar
            {
                Style = (Style)FindResource("MetricBar"),
                Height = compact ? 3.5 : 4,
                Margin = new Thickness(0, compact ? 3 : 4, 0, 0),
                Maximum = 100,
                Value = drive.Percent,
                Foreground = drive.Percent >= 90 ? StatusRed
                    : drive.Percent >= 75 ? StatusAmber
                    : _barNormal
            });

            target.Children.Add(panel);
        }
    }

    private void DetectLocalIpChange(string ip)
    {
        if (string.IsNullOrWhiteSpace(ip) || ip == "—")
            return;

        if (_lastLocalIp.Length > 0 && ip != _lastLocalIp)
            ShowBalloon("Yerel IP değişti", $"{_lastLocalIp} → {ip}", Forms.ToolTipIcon.Info);

        _lastLocalIp = ip;
    }

    private void CopyMainIp_Click(object sender, RoutedEventArgs e) =>
        CopyIpToClipboard(MainIpText.Text, "yerel IP");

    private void CopyMainPublicIp_Click(object sender, RoutedEventArgs e) =>
        CopyIpToClipboard(MainPublicIpText.Text, "genel IP");

    private void CopyIpToClipboard(string ip, string label)
    {
        if (ip is "—" or "")
        {
            MainConnStatus.Text = "IP henüz alınmadı";
            MainConnStatus.Foreground = StatusAmber;
            return;
        }

        try
        {
            Clipboard.SetText(ip);
            MainConnStatus.Text = $"{label} kopyalandı";
            MainConnStatus.Foreground = StatusGreen;
        }
        catch
        {
            MainConnStatus.Text = "panoya kopyalanamadı";
            MainConnStatus.Foreground = StatusRed;
        }
    }

    private async Task RefreshPublicIpAsync()
    {
        if (_ipLoading)
            return;

        _ipLoading = true;
        _lastPublicIpCheck = DateTime.Now;
        try
        {
            var ip = await ToolsService.GetPublicIpAsync(_http, CancellationToken.None);
            if (ip == null)
                return;

            _publicIp = ip;
            MainPublicIpText.Text = ip;
            ToolPublicIpText.Text = ip;
            InternetBox.ToolTip =
                $"Yerel IP: {NetworkMonitor.GetLocalIp()}\nDış IP: {_publicIp}\nHedef: 1.1.1.1";

            if (_lastPublicIp.Length > 0 && ip != _lastPublicIp)
                ShowBalloon("Genel IP değişti", $"{_lastPublicIp} → {ip}", Forms.ToolTipIcon.Info);
            _lastPublicIp = ip;
        }
        finally
        {
            _ipLoading = false;
        }
    }

    private void SetToolsStatus(string text, Brush? brush = null)
    {
        ToolsStatusText.Text = text;
        ToolsStatusText.Foreground = brush ?? Muted;
    }

    private void CopyPublicIp_Click(object sender, RoutedEventArgs e)
    {
        if (_publicIp is "—" or "")
        {
            SetToolsStatus("genel IP henüz alınmadı", StatusAmber);
            return;
        }

        try
        {
            Clipboard.SetText(_publicIp);
            SetToolsStatus("genel IP kopyalandı · " + _publicIp, StatusGreen);
        }
        catch
        {
            SetToolsStatus("panoya kopyalanamadı", StatusRed);
        }
    }

    private async void PingTool_Click(object sender, RoutedEventArgs e)
    {
        var host = ToolsHost.Text.Trim();
        if (host.Length == 0)
        {
            PingResultText.Text = "sunucu adı veya IP girin";
            PingResultText.Foreground = StatusAmber;
            return;
        }

        PingResultText.Text = "ping atılıyor…";
        PingResultText.Foreground = Muted;

        var sample = await ToolsService.PingAsync(host, 4);
        if (sample == null || sample.Received == 0)
        {
            PingResultText.Text = $"{host} · yanıt yok";
            PingResultText.Foreground = StatusRed;
            return;
        }

        PingResultText.Text =
            $"{sample.Host} ({sample.Address}) · {sample.Received}/{sample.Sent} yanıt · " +
            $"ort {sample.Avg} ms (min {sample.Min} / maks {sample.Max})";
        PingResultText.Foreground = sample.Avg <= 80 ? StatusGreen : sample.Avg <= 200 ? StatusAmber : StatusRed;
    }

    private async void PortTool_Click(object sender, RoutedEventArgs e)
    {
        var host = ToolsPortHost.Text.Trim();
        if (host.Length == 0 ||
            !int.TryParse(ToolsPort.Text.Trim(), NumberStyles.Integer, Tr, out var port) ||
            port is < 1 or > 65535)
        {
            PortResultText.Text = "geçerli bir host ve port girin";
            PortResultText.Foreground = StatusAmber;
            return;
        }

        PortResultText.Text = $"{host}:{port} deneniyor…";
        PortResultText.Foreground = Muted;

        var watch = Stopwatch.StartNew();
        var open = await ToolsService.IsPortOpenAsync(host, port, CancellationToken.None);
        watch.Stop();

        PortResultText.Text = $"{host}:{port} · {(open ? "AÇIK" : "kapalı")} · {watch.ElapsedMilliseconds} ms";
        PortResultText.Foreground = open ? StatusGreen : StatusRed;
    }

    private void QuickLaunch_Click(object sender, RoutedEventArgs e)
    {
        if (sender is not Button { Tag: string file })
            return;

        ToolsService.Launch(file);
        SetToolsStatus($"{file} başlatıldı", StatusGreen);
    }

    private void CopyDiagnostics_Click(object sender, RoutedEventArgs e)
    {
        var connection = ToolsService.GetConnection();
        var battery = ToolsService.GetBattery();
        var lines = new List<string>
        {
            $"YEK Panel v2.0 · {DateTime.Now.ToString("dd.MM.yyyy HH:mm", Tr)}",
            $"Bilgisayar: {Environment.MachineName} · {Environment.OSVersion.VersionString}",
            $"CPU %{_lastCpuPercent.ToString("0", Tr)} · RAM %{_lastRamPercent.ToString("0", Tr)} · " +
            $"Disk %{_lastDiskPercent.ToString("0", Tr)}",
            $"Bağlantı: {connection.Adapter} · IP {connection.Ipv4}",
            $"Ağ geçidi: {connection.Gateway} · DNS: {connection.Dns}",
            $"Kablosuz: {connection.Wifi} · Genel IP: {_publicIp}",
            $"Pil: {battery.Text}"
        };

        foreach (var row in _rows.Where(r => !string.IsNullOrWhiteSpace(r.Url)))
            lines.Add($"{row.Name}: {row.Status.Text} · {row.Url}");

        try
        {
            Clipboard.SetText(string.Join("\n", lines));
            SetToolsStatus("tanılama panoya kopyalandı", StatusGreen);
        }
        catch
        {
            SetToolsStatus("panoya kopyalanamadı", StatusRed);
        }
    }

    private void StopwatchToggle_Click(object sender, RoutedEventArgs e)
    {
        if (_stopwatch.IsRunning)
        {
            _stopwatch.Stop();
            _stopwatchTimer.Stop();
            StopwatchButton.Content = "DEVAM";
            System.Windows.Automation.AutomationProperties.SetName(StopwatchButton, "Kronometre devam");
        }
        else
        {
            _stopwatch.Start();
            _stopwatchTimer.Start();
            StopwatchButton.Content = "DUR";
            System.Windows.Automation.AutomationProperties.SetName(StopwatchButton, "Kronometre durdur");
        }

        UpdateStopwatch();
    }

    private void UpdateStopwatch()
    {
        var elapsed = _stopwatch.Elapsed;
        StopwatchText.Text = elapsed.TotalHours >= 1
            ? $"{(int)elapsed.TotalHours:00}:{elapsed.Minutes:00}:{elapsed.Seconds:00}.{elapsed.Milliseconds / 100}"
            : $"{elapsed.Minutes:00}:{elapsed.Seconds:00}.{elapsed.Milliseconds / 100}";
    }

    private void StopwatchReset_Click(object sender, RoutedEventArgs e)
    {
        _stopwatch.Reset();
        _stopwatchTimer.Stop();
        StopwatchButton.Content = "BAŞLAT";
        System.Windows.Automation.AutomationProperties.SetName(StopwatchButton, "Kronometre başlat");
        UpdateStopwatch();
    }

    private void CountdownStart_Click(object sender, RoutedEventArgs e)
    {
        if (!int.TryParse(CountdownMinutes.Text.Trim(), NumberStyles.Integer, Tr, out var minutes) ||
            minutes is < 1 or > 600)
        {
            CountdownText.Text = "1-600 dakika arası girin";
            CountdownText.Foreground = StatusAmber;
            return;
        }

        _countdownEnd = DateTime.Now.AddMinutes(minutes);
        _countdownTimer.Start();
        UpdateCountdown();
    }

    private void UpdateCountdown()
    {
        if (_countdownEnd is not DateTime end)
        {
            CountdownText.Text = "geri sayım kapalı";
            CountdownText.Foreground = Muted;
            return;
        }

        var left = end - DateTime.Now;
        if (left <= TimeSpan.Zero)
        {
            _countdownEnd = null;
            _countdownTimer.Stop();
            CountdownText.Text = "süre doldu";
            CountdownText.Foreground = StatusGreen;
            ShowBalloon(
                "YEK Panel · Geri sayım",
                "Ayarladığınız süre doldu.",
                Forms.ToolTipIcon.Info,
                force: true);
            return;
        }

        CountdownText.Text = $"kalan {left.Hours:00}:{left.Minutes:00}:{left.Seconds:00}";
        CountdownText.Foreground = left.TotalMinutes <= 1 ? StatusAmber : TextSoft;
    }

    private void SaveNote_Click(object sender, RoutedEventArgs e)
    {
        _settings.QuickNote = NoteBox.Text.Trim();
        SettingsStore.Save(_settings);
        SetToolsStatus($"not kaydedildi · {_settings.QuickNote.Length} karakter", StatusGreen);
    }

    private void BackupSettings_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            SettingsStore.Save(_settings);
            var dialog = new SaveFileDialog
            {
                Title = "YEK Panel ayarlarını yedekle",
                Filter = "JSON (*.json)|*.json",
                FileName = $"YekPanel-ayarlar-{DateTime.Now:yyyyMMdd-HHmm}.json",
                InitialDirectory = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments)
            };

            if (dialog.ShowDialog(this) != true)
                return;

            File.Copy(SettingsStore.FilePathPublic, dialog.FileName, overwrite: true);
            SetToolsStatus("yedek kaydedildi: " + dialog.FileName, StatusGreen);
        }
        catch
        {
            SetToolsStatus("ayarlar yedeklenemedi", StatusRed);
        }
    }

    private void RestoreSettings_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog
        {
            Title = "YEK Panel ayar yedeği seç",
            Filter = "JSON (*.json)|*.json|Tüm dosyalar (*.*)|*.*",
            CheckFileExists = true
        };

        if (dialog.ShowDialog(this) != true)
            return;

        if (MessageBox.Show(
                this,
                "Ayarlar seçilen dosyadan yüklenecek ve uygulama yeniden başlatılacak. Devam edilsin mi?",
                "Ayarları geri yükle",
                MessageBoxButton.YesNo,
                MessageBoxImage.Question) != MessageBoxResult.Yes)
            return;

        if (!SettingsStore.ReplaceFromFile(dialog.FileName))
        {
            SetToolsStatus("geçersiz ayar dosyası", StatusRed);
            return;
        }

        try
        {
            var path = Environment.ProcessPath;
            if (string.IsNullOrEmpty(path))
                return;

            Process.Start(new ProcessStartInfo { FileName = path, UseShellExecute = true });
            Close();
        }
        catch
        {
            SetToolsStatus("yeniden başlatılamadı; uygulamayı elle açın", StatusRed);
        }
    }

    private void ClearData_Click(object sender, RoutedEventArgs e)
    {
        if (MessageBox.Show(
                this,
                "Çevrimiçilik geçmişi ve bildirim kayıtları silinsin mi? Ayarlar ve hızlı not korunur.",
                "Verileri temizle",
                MessageBoxButton.YesNo,
                MessageBoxImage.Warning) != MessageBoxResult.Yes)
            return;

        UptimeHistory.Clear();
        NotificationLog.Clear();
        BuildServerRows();
        if (HistoryOverlay.Visibility == Visibility.Visible)
            BuildHistoryReport();

        SetToolsStatus("geçmiş ve bildirim kayıtları temizlendi", StatusGreen);
    }

    private void TestNotification_Click(object sender, RoutedEventArgs e)
    {
        if (_tray == null)
        {
            SetToolsStatus("tepsi simgesi bulunamadı", StatusRed);
            return;
        }

        ShowBalloon(
            "YEK Panel · Bildirim testi",
            "Bildirimler çalışıyor. Bu bir deneme mesajıdır.",
            Forms.ToolTipIcon.Info,
            force: true);
        SetToolsStatus("test bildirimi gönderildi", StatusGreen);
    }

    private void OpenFeatures_Click(object sender, RoutedEventArgs e)
    {
        var path = ToolsService.FindDataFile("OZELLIKLER.txt");
        if (path == null)
        {
            SetToolsStatus("OZELLIKLER.txt bulunamadı", StatusAmber);
            return;
        }

        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = "notepad.exe",
                Arguments = $"\"{path}\"",
                UseShellExecute = true
            });
            SetToolsStatus("özellik listesi açıldı", StatusGreen);
        }
        catch
        {
            ToolsService.Launch(path);
        }
    }

    private void Report7_Click(object sender, RoutedEventArgs e)
    {
        _reportDays = 7;
        BuildHistoryReport();
    }

    private void Report30_Click(object sender, RoutedEventArgs e)
    {
        _reportDays = 30;
        BuildHistoryReport();
    }

    private void OpacityDown_Click(object sender, RoutedEventArgs e) => ApplyOpacity(_pendingOpacity - 0.05);

    private void OpacityUp_Click(object sender, RoutedEventArgs e) => ApplyOpacity(_pendingOpacity + 0.05);

    private void ApplyOpacity(double value)
    {
        _pendingOpacity = Math.Clamp(Math.Round(value, 2), 0.6, 1.0);
        Opacity = _pendingOpacity;
        OpacityValue.Text = $"%{(_pendingOpacity * 100).ToString("0", Tr)}";
    }

    private void ShowBalloon(string title, string text, Forms.ToolTipIcon icon, bool force = false)
    {
        if (!force && (!_settings.NotificationsEnabled || IsQuietHours(DateTime.Now)))
            return;

        if (_tray == null || !_tray.Visible)
            return;

        try
        {
            _tray.ShowBalloonTip(6000, title, text, icon);
        }
        catch
        {
        }
    }

    private void CheckDailySummary()
    {
        if (!_settings.DailySummaryEnabled || !_settings.NotificationsEnabled)
            return;

        if (!TimeSpan.TryParseExact(
                _settings.DailySummaryTime,
                @"hh\:mm",
                CultureInfo.InvariantCulture,
                out var target))
            return;

        var now = DateTime.Now;
        if (now.TimeOfDay < target)
            return;

        var today = now.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        if (_settings.LastSummaryDate == today)
            return;

        _settings.LastSummaryDate = today;
        SettingsStore.Save(_settings);

        var issues = NotificationLog.CountIssuesSince(DateTime.UtcNow.AddHours(-24));
        ShowBalloon(
            "YEK Panel · Günlük özet",
            $"{_serverSummaryText} · 24 saatte {issues} sorun · " +
            $"CPU %{_lastCpuPercent.ToString("0", Tr)} · RAM %{_lastRamPercent.ToString("0", Tr)}",
            Forms.ToolTipIcon.Info);
    }

    private sealed class ServerRow
    {
        public required Border Root { get; init; }
        public required Ellipse Dot { get; init; }
        public required TextBlock Latency { get; init; }
        public required TextBlock Status { get; init; }
        public required string Name { get; init; }
        public required string Url { get; init; }
        public required string Mac { get; init; }
        public required Button Wol { get; init; }
        public required bool Maintenance { get; init; }
        public required List<Border> HistoryBars { get; init; }
        public required Canvas LatencyChart { get; init; }
        public CertificateInfo? Cert { get; set; }
        public string Detail { get; set; } = "";
        public string PortLine { get; set; } = "";
        public string RatioLine { get; set; } = "";
        public List<int> History { get; } = new();
        public List<long> Latencies { get; } = new();
        public int LastState { get; set; }
    }

    private sealed record ProcessRow(TextBlock Name, TextBlock Value);
}
