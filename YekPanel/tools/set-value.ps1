param(
    [Parameter(Mandatory = $true)][string]$ProcessName,
    [Parameter(Mandatory = $true)][string]$AutomationId,
    [Parameter(Mandatory = $true)][string]$Value
)
Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes
$proc = Get-Process -Name $ProcessName -ErrorAction Stop | Select-Object -First 1
$win = [System.Windows.Automation.AutomationElement]::FromHandle($proc.MainWindowHandle)
$cond = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::AutomationIdProperty, $AutomationId)
$el = $win.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $cond)
if ($null -eq $el) { Write-Output "Bulunamadi: $AutomationId"; exit 1 }
$pattern = $el.GetCurrentPattern([System.Windows.Automation.ValuePattern]::Pattern)
$pattern.SetValue($Value)
Write-Output "Deger yazildi: $AutomationId = $Value"
