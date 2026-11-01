param(
    [Parameter(Mandatory = $true)][string]$ProcessName
)

Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

$proc = Get-Process -Name $ProcessName -ErrorAction Stop | Select-Object -First 1
$win = [System.Windows.Automation.AutomationElement]::FromHandle($proc.MainWindowHandle)

$textCond = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
    [System.Windows.Automation.ControlType]::Text)
$buttonCond = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::ControlTypeProperty,
    [System.Windows.Automation.ControlType]::Button)

Write-Output "--- METINLER ---"
$win.FindAll([System.Windows.Automation.TreeScope]::Descendants, $textCond) | ForEach-Object {
    Write-Output ("TEXT: [" + $_.Current.Name + "]")
}

Write-Output "--- BUTONLAR ---"
$win.FindAll([System.Windows.Automation.TreeScope]::Descendants, $buttonCond) | ForEach-Object {
    Write-Output ("BUTTON: [" + $_.Current.Name + "]")
}
