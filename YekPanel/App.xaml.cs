using System.Globalization;
using System.Windows;

namespace YekPanel;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        var tr = new CultureInfo("tr-TR");
        CultureInfo.DefaultThreadCurrentCulture = tr;
        CultureInfo.DefaultThreadCurrentUICulture = tr;
        base.OnStartup(e);
    }
}
