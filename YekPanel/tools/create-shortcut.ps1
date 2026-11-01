param(
    [Parameter(Mandatory = $true)][string]$Exe
)

$exePath = (Resolve-Path $Exe).Path
$workDir = Split-Path $exePath -Parent
$desktop = [Environment]::GetFolderPath('Desktop')
$linkPath = Join-Path $desktop 'YEK Panel.lnk'

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($linkPath)
$shortcut.TargetPath = $exePath
$shortcut.WorkingDirectory = $workDir
$shortcut.IconLocation = $exePath + ',0'
$shortcut.Description = 'YEK Panel - Sistem ve sunucu izleme'
$shortcut.Save()

Write-Output ("Kisayol: " + $linkPath)
