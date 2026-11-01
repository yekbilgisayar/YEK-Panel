param(
    [string]$ProcessName = "YekPanel",
    [double]$Percent = 100,
    [string]$Shot = ""
)

Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

$proc = Get-Process -Name $ProcessName -ErrorAction Stop | Select-Object -First 1
$root = [System.Windows.Automation.AutomationElement]::RootElement
$cond = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::ProcessIdProperty, $proc.Id)
$window = $root.FindFirst([System.Windows.Automation.TreeScope]::Children, $cond)
if ($null -eq $window) { throw "Pencere bulunamadi" }

$scrollCond = New-Object System.Windows.Automation.PropertyCondition(
    [System.Windows.Automation.AutomationElement]::IsScrollPatternAvailableProperty, $true)
$all = $window.FindAll([System.Windows.Automation.TreeScope]::Descendants, $scrollCond)

$done = 0
foreach ($el in $all) {
    $pattern = $null
    if ($el.TryGetCurrentPattern([System.Windows.Automation.ScrollPattern]::Pattern, [ref]$pattern)) {
        try {
            if ($pattern.Current.VerticallyScrollable) {
                $pattern.SetScrollPercent([System.Windows.Automation.ScrollPattern]::NoScroll, $Percent)
                $done++
            }
        } catch {
        }
    }
}

Start-Sleep -Milliseconds 400
Write-Output "Kaydirilan alan: $done"

if ($Shot) {
    & (Join-Path $PSScriptRoot "shot-window.ps1") -ProcessName $ProcessName -Shot $Shot
}
