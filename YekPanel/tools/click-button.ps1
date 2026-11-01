param(
    [Parameter(Mandatory = $true)][string]$ProcessName,
    [Parameter(Mandatory = $true)][string]$ButtonName
)

Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

$proc = Get-Process -Name $ProcessName -ErrorAction Stop | Select-Object -First 1
$win = [System.Windows.Automation.AutomationElement]::FromHandle($proc.MainWindowHandle)
$cond = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::NameProperty, $ButtonName)
$el = $win.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $cond)

if ($null -eq $el) {
    Write-Output "Buton bulunamadi: $ButtonName"
    exit 1
}

$invoke = $el.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern)
$invoke.Invoke()
Write-Output "Tiklandi: $ButtonName"
