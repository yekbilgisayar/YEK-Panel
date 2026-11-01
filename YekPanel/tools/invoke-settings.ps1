param(
    [Parameter(Mandatory = $true)][string]$ProcessName,
    [Parameter(Mandatory = $true)][string]$Shot
)

Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

Add-Type @"
using System;
using System.Runtime.InteropServices;
public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
public static class Win32 {
    [DllImport("user32.dll")] private static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    public static RECT Rect(IntPtr hWnd) { RECT r; GetWindowRect(hWnd, out r); return r; }
}
"@

$proc = Get-Process -Name $ProcessName -ErrorAction Stop | Select-Object -First 1
$hwnd = $proc.MainWindowHandle
[Win32]::SetForegroundWindow($hwnd) | Out-Null
Start-Sleep -Milliseconds 400

$win = [System.Windows.Automation.AutomationElement]::FromHandle($hwnd)
$winRect = $win.Current.BoundingRectangle

$buttonCond = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
    [System.Windows.Automation.ControlType]::Button)

$candidates = @()
foreach ($b in $win.FindAll([System.Windows.Automation.TreeScope]::Descendants, $buttonCond)) {
    try {
        $pattern = $b.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern)
        if ($null -eq $pattern) { continue }
        $r = $b.Current.BoundingRectangle
        if ($r.Width -le 0 -or $r.Height -le 0) { continue }
        $bottomZone = $winRect.Top + $winRect.Height - 60
        if ($r.Top -ge $bottomZone) {
            $candidates += [pscustomobject]@{ Element = $b; Pattern = $pattern; X = $r.X + $r.Width / 2; Y = $r.Y + $r.Height / 2 }
        }
    } catch { }
}

if ($candidates.Count -eq 0) { Write-Output "Ayar dugmesi bulunamadi"; exit 1 }

$target = $candidates | Sort-Object X -Descending | Select-Object -First 1
$target.Pattern.Invoke()
Start-Sleep -Milliseconds 600

$rect = [Win32]::Rect($hwnd)
$width = $rect.Right - $rect.Left
$height = $rect.Bottom - $rect.Top
$bmp = New-Object System.Drawing.Bitmap $width, $height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($rect.Left, $rect.Top, 0, 0, (New-Object System.Drawing.Size $width, $height))
$g.Dispose()
$bmp.Save($Shot, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output ("Ayar dugmesi tiklandi: " + [int]$target.X + "," + [int]$target.Y)
