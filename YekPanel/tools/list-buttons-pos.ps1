param([string]$ProcessName="YekPanel")
Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes
$proc=Get-Process -Name $ProcessName -ErrorAction Stop | Select-Object -First 1
$win=[System.Windows.Automation.AutomationElement]::FromHandle($proc.MainWindowHandle)
$r=$win.Current.BoundingRectangle
Write-Output ("WIN: " + [int]$r.Left + "," + [int]$r.Top + " " + [int]$r.Width + "x" + [int]$r.Height)
$cond=New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::ControlTypeProperty,[System.Windows.Automation.ControlType]::Button)
$win.FindAll([System.Windows.Automation.TreeScope]::Descendants,$cond) | ForEach-Object {
  $b=$_.Current.BoundingRectangle
  if($b.Height -gt 0){ Write-Output ("[" + $_.Current.Name + "] x=" + [int]($b.X+$b.Width/2) + " y=" + [int]($b.Y+$b.Height/2)) }
}
