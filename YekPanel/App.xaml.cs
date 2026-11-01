using System.Globalization;
using System.Threading;
using System.Windows;
using System.Windows.Threading;
using YekPanel.Services;

namespace YekPanel;

public partial class App : Application
{
    private const string MutexName = @"Local\YekPanel.SingleInstance";
    private const string ActivateEventName = @"Local\YekPanel.Activate";

    private Mutex? _mutex;
    private EventWaitHandle? _activateEvent;
    private RegisteredWaitHandle? _activateWait;
    private MainWindow? _mainWindow;
    private bool _showingCrash;

    protected override void OnStartup(StartupEventArgs e)
    {
        var tr = new CultureInfo("tr-TR");
        CultureInfo.DefaultThreadCurrentCulture = tr;
        CultureInfo.DefaultThreadCurrentUICulture = tr;

        base.OnStartup(e);

        DispatcherUnhandledException += OnDispatcherUnhandledException;
        AppDomain.CurrentDomain.UnhandledException += OnDomainUnhandledException;
        TaskScheduler.UnobservedTaskException += OnUnobservedTaskException;

        _mutex = new Mutex(initiallyOwned: true, MutexName, out var isFirstInstance);
        if (!isFirstInstance)
        {
            // Zaten çalışan örnek varsa onu öne getir ve bu örneği kapat.
            SignalExistingInstance();
            Shutdown();
            return;
        }

        _activateEvent = new EventWaitHandle(false, EventResetMode.AutoReset, ActivateEventName);
        _activateWait = ThreadPool.RegisterWaitForSingleObject(
            _activateEvent,
            (_, _) => Dispatcher.BeginInvoke(new Action(() => _mainWindow?.BringToFront())),
            null,
            Timeout.Infinite,
            executeOnlyOnce: false);

        _mainWindow = new MainWindow();
        MainWindow = _mainWindow;
        _mainWindow.Show();
    }

    private static void SignalExistingInstance()
    {
        try
        {
            using var handle = EventWaitHandle.OpenExisting(ActivateEventName);
            handle.Set();
        }
        catch
        {
            // Eski sürüm çalışıyorsa uyarı olayı yoktur; sessizce kapan.
        }
    }

    private void OnDispatcherUnhandledException(object sender, DispatcherUnhandledExceptionEventArgs e)
    {
        AppLog.Error("Arayüz hatası", e.Exception);
        e.Handled = true;

        if (_showingCrash)
            return;

        try
        {
            _showingCrash = true;
            MessageBox.Show(
                "Beklenmeyen bir hata oluştu. Uygulama çalışmaya devam ediyor.\n\n" +
                $"Ayrıntılar: {AppLog.FilePath}",
                "YEK Panel",
                MessageBoxButton.OK,
                MessageBoxImage.Warning);
        }
        catch
        {
        }
        finally
        {
            _showingCrash = false;
        }
    }

    private static void OnDomainUnhandledException(object sender, UnhandledExceptionEventArgs e)
    {
        if (e.ExceptionObject is Exception exception)
            AppLog.Error("Uygulama hatası", exception);
    }

    private static void OnUnobservedTaskException(object? sender, UnobservedTaskExceptionEventArgs e)
    {
        AppLog.Error("Arka plan görevi", e.Exception);
        e.SetObserved();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        try
        {
            _activateWait?.Unregister(null);
        }
        catch
        {
        }

        _activateEvent?.Dispose();
        _activateEvent = null;

        try
        {
            _mutex?.ReleaseMutex();
        }
        catch
        {
        }

        _mutex?.Dispose();
        _mutex = null;

        base.OnExit(e);
    }
}
