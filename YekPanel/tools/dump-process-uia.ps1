param(
    [Parameter(Mandatory = $true)][int]$ProcessId
)

Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

$root = [System.Windows.Automation.AutomationElement]::RootElement
$cond = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::ProcessIdProperty, $ProcessId)
$windows = $root.FindAll([System.Windows.Automation.TreeScope]::Children, $cond)

Write-Output ("Pencere sayisi: " + $windows.Count)
foreach ($w in $windows) {
    Write-Output ("WINDOW: [" + $w.Current.Name + "] class=" + $w.Current.ClassName + " handle=" + $w.Current.NativeWindowHandle)
    $all = $w.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
    foreach ($el in $all) {
        $type = $el.Current.ControlType.ProgrammaticName
        if ($el.Current.Name -or $type -match 'Button|Edit|CheckBox') {
            Write-Output ("  " + $type + " [" + $el.Current.Name + "] id=" + $el.Current.AutomationId + " enabled=" + $el.Current.IsEnabled)
        }
    }
}
