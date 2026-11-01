param([string]$ProcessName = "YekPanel")
Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes
$proc = Get-Process -Name $ProcessName -ErrorAction Stop | Select-Object -First 1
$win = [System.Windows.Automation.AutomationElement]::FromHandle($proc.MainWindowHandle)
$cond = New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::ControlTypeProperty, [System.Windows.Automation.ControlType]::Edit)
$all = $win.FindAll([System.Windows.Automation.TreeScope]::Descendants, $cond)
foreach ($el in $all) { Write-Output ("Id=" + $el.Current.AutomationId + " | Name=" + $el.Current.Name) }
Write-Output ("Toplam: " + $all.Count)
