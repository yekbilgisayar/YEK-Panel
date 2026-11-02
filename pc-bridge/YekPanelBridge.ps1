#requires -version 5.1
<#
  YEK Panel PC Köprüsü
  Windows bilgisayarın sistem verilerini yerel ağdaki YEK Panel Android
  uygulamasına açar. Veriler yalnızca LAN üzerinde, 6 haneli PIN ile sunulur.
  Hiçbir veri internete gönderilmez.
#>
[CmdletBinding()]
param(
    [int]$Port = 8787,
    [int]$DiscoveryPort = 8788,
    [string]$Pin = "",
    [switch]$NoDiscovery
)

$ErrorActionPreference = 'Stop'
$script:Version = '1.0.0'
$script:AppName = 'YEK Panel PC Köprüsü'
$script:DataDir = Join-Path $env:APPDATA 'YekPanelBridge'
$script:ConfigPath = Join-Path $script:DataDir 'config.json'
$script:StartedAt = Get-Date
$script:prevCpu = $null
$script:prevNet = $null
$script:prevNetAt = $null
$script:lastTemp = $null
$script:lastTempAt = [datetime]::MinValue
$script:authFails = @{}
$script:OsName = ''
$script:PcName = $env:COMPUTERNAME
$script:PinGiven = $PSBoundParameters.ContainsKey('Pin')
$script:PortGiven = $PSBoundParameters.ContainsKey('Port')

# --------------------------------------------------------------- Yerel API
try {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

namespace YekBridge
{
    [StructLayout(LayoutKind.Sequential)]
    public struct FILETIME
    {
        public uint Low;
        public uint High;
    }

    [StructLayout(LayoutKind.Sequential)]
    public class MEMORYSTATUSEX
    {
        public uint dwLength;
        public uint dwMemoryLoad;
        public ulong ullTotalPhys;
        public ulong ullAvailPhys;
        public ulong ullTotalPageFile;
        public ulong ullAvailPageFile;
        public ulong ullTotalVirtual;
        public ulong ullAvailVirtual;
        public ulong ullAvailExtendedVirtual;

        public MEMORYSTATUSEX()
        {
            dwLength = (uint)Marshal.SizeOf(typeof(MEMORYSTATUSEX));
        }
    }

    public static class Native
    {
        [DllImport("kernel32.dll", SetLastError = true)]
        public static extern bool GetSystemTimes(
            out FILETIME idle, out FILETIME kernel, out FILETIME user);

        [DllImport("kernel32.dll", SetLastError = true)]
        public static extern bool GlobalMemoryStatusEx([In, Out] MEMORYSTATUSEX buffer);

        [DllImport("kernel32.dll")]
        public static extern ulong GetTickCount64();
    }
}
'@
} catch {
    # Aynı oturumda ikinci kez yükleniyorsa tür zaten vardır.
}

function Get-YekPin {
    $random = New-Object System.Random
    $first = $random.Next(1, 10)
    $rest = -join (1..5 | ForEach-Object { $random.Next(0, 10) })
    return "$first$rest"
}

function Initialize-YekConfig {
    if (-not (Test-Path -LiteralPath $script:DataDir)) {
        New-Item -ItemType Directory -Path $script:DataDir -Force | Out-Null
    }
    $config = $null
    if (Test-Path -LiteralPath $script:ConfigPath) {
        try {
            $config = Get-Content -LiteralPath $script:ConfigPath -Raw -Encoding UTF8 |
                ConvertFrom-Json
        } catch {
            $config = $null
        }
    }
    if (-not $config) {
        $config = [pscustomobject]@{
            pin = Get-YekPin
            port = $script:Port
            createdAt = (Get-Date).ToString('s')
        }
    }
    if (-not $script:PinGiven -and [string]::IsNullOrWhiteSpace($Pin)) {
        $Pin = [string]$config.pin
    }
    if ($Pin -notmatch '^\d{6}$') {
        $Pin = Get-YekPin
    }
    if (-not $script:PortGiven -and $config.port) {
        $script:Port = [int]$config.port
    }
    $config.pin = $Pin
    $config.port = $script:Port
    try {
        $config | ConvertTo-Json -Compress | Set-Content -LiteralPath $script:ConfigPath `
            -Encoding UTF8
    } catch {
    }
    $script:Pin = $Pin
}

function Get-YekCpuPercent {
    try {
        $idle = New-Object YekBridge.FILETIME
        $kernel = New-Object YekBridge.FILETIME
        $user = New-Object YekBridge.FILETIME
        if (-not [YekBridge.Native]::GetSystemTimes(
                [ref]$idle, [ref]$kernel, [ref]$user)) {
            return 0.0
        }
        $i = [uint64]$idle.High * 4294967296 + [uint64]$idle.Low
        $k = [uint64]$kernel.High * 4294967296 + [uint64]$kernel.Low
        $u = [uint64]$user.High * 4294967296 + [uint64]$user.Low
        if ($null -eq $script:prevCpu) {
            $script:prevCpu = @($i, $k, $u)
            return 0.0
        }
        $di = $i - $script:prevCpu[0]
        $dk = $k - $script:prevCpu[1]
        $du = $u - $script:prevCpu[2]
        $script:prevCpu = @($i, $k, $u)
        $total = $dk + $du
        if ($total -le 0) {
            return 0.0
        }
        $busy = $total - $di
        return [math]::Round([math]::Max(0.0, [math]::Min(100.0, $busy * 100.0 / $total)), 1)
    } catch {
        return 0.0
    }
}

function Get-YekMemory {
    try {
        $mem = New-Object YekBridge.MEMORYSTATUSEX
        if (-not [YekBridge.Native]::GlobalMemoryStatusEx($mem)) {
            return @{ total = 0; used = 0; percent = 0 }
        }
        $total = [int64]$mem.ullTotalPhys
        $available = [int64]$mem.ullAvailPhys
        $used = [math]::Max([int64]0, $total - $available)
        $percent = if ($total -gt 0) {
            [int][math]::Round($used * 100.0 / $total)
        } else {
            0
        }
        return @{ total = $total; used = $used; percent = $percent }
    } catch {
        return @{ total = 0; used = 0; percent = 0 }
    }
}

function Get-YekNetwork {
    $received = [int64]0
    $sent = [int64]0
    $link = [int64]0
    $ips = New-Object System.Collections.ArrayList
    try {
        $nics = [System.Net.NetworkInformation.NetworkInterface]::GetAllNetworkInterfaces()
        foreach ($nic in $nics) {
            if ($nic.OperationalStatus -ne [System.Net.NetworkInformation.OperationalStatus]::Up) {
                continue
            }
            if ($nic.NetworkInterfaceType -eq [System.Net.NetworkInformation.NetworkInterfaceType]::Loopback -or
                $nic.NetworkInterfaceType -eq [System.Net.NetworkInformation.NetworkInterfaceType]::Tunnel) {
                continue
            }
            try {
                $stats = $nic.GetIPv4Statistics()
                $received += [int64]$stats.BytesReceived
                $sent += [int64]$stats.BytesSent
            } catch {
            }
            $virtualName = $false
            try {
                $virtualName = $nic.Name -match 'vEthernet|Hyper-V|Virtual|Loopback|Teredo|VPN|TAP|WSL|Tailscale|VMware|VMnet'
                if (-not $virtualName) {
                    $speed = [int64]$nic.Speed
                    if ($speed -gt $link -and $speed -lt 100000000000) {
                        $link = $speed
                    }
                }
            } catch {
            }
            try {
                foreach ($address in $nic.GetIPProperties().UnicastAddresses) {
                    $text = $address.Address.ToString()
                    if ($address.Address.AddressFamily -eq
                        [System.Net.Sockets.AddressFamily]::InterNetwork -and
                        -not $text.StartsWith('127.') -and
                        -not $text.StartsWith('169.254.') -and
                        -not $virtualName) {
                        if (-not $ips.Contains($text)) {
                            [void]$ips.Add($text)
                        }
                    }
                }
            } catch {
            }
        }
    } catch {
    }

    $now = Get-Date
    $down = [int64]0
    $up = [int64]0
    if ($null -ne $script:prevNet -and $null -ne $script:prevNetAt) {
        $seconds = ($now - $script:prevNetAt).TotalSeconds
        if ($seconds -gt 0.2) {
            $down = [int64][math]::Max(0, ($received - $script:prevNet[0]) / $seconds)
            $up = [int64][math]::Max(0, ($sent - $script:prevNet[1]) / $seconds)
        }
    }
    $script:prevNet = @($received, $sent)
    $script:prevNetAt = $now

    return @{
        down = $down
        up = $up
        link = [int64][math]::Round($link / 1000000.0)
        ips = @($ips.ToArray())
    }
}

function Get-YekDisks {
    $list = New-Object System.Collections.ArrayList
    try {
        foreach ($drive in [System.IO.DriveInfo]::GetDrives()) {
            try {
                if (-not $drive.IsReady -or
                    $drive.DriveType -ne [System.IO.DriveType]::Fixed) {
                    continue
                }
                $total = [int64]$drive.TotalSize
                $free = [int64]$drive.AvailableFreeSpace
                $percent = if ($total -gt 0) {
                    [int][math]::Round(($total - $free) * 100.0 / $total)
                } else {
                    0
                }
                [void]$list.Add([ordered]@{
                    name = $drive.Name.TrimEnd('\')
                    total = $total
                    free = $free
                    percent = $percent
                })
            } catch {
            }
        }
    } catch {
    }
    return @($list.ToArray())
}

function Get-YekBattery {
    $percent = -1
    $charging = $false
    try {
        $battery = Get-CimInstance -ClassName Win32_Battery -ErrorAction Stop |
            Select-Object -First 1
        if ($null -ne $battery) {
            $percent = [int]$battery.EstimatedChargeRemaining
            $status = [int]$battery.BatteryStatus
            $charging = @(2, 6, 7, 8, 9) -contains $status
        }
    } catch {
    }
    return @{ percent = $percent; charging = $charging }
}

function Get-YekTemperature {
    $now = Get-Date
    if (($now - $script:lastTempAt).TotalSeconds -lt 10) {
        return $script:lastTemp
    }
    $script:lastTempAt = $now
    try {
        $zones = Get-CimInstance -Namespace 'root/wmi' `
            -ClassName MSAcpi_ThermalZoneTemperature -ErrorAction Stop
        $temperatures = New-Object System.Collections.ArrayList
        foreach ($zone in $zones) {
            $celsius = ($zone.CurrentTemperature / 10.0) - 273.15
            if ($celsius -gt -40 -and $celsius -lt 150) {
                [void]$temperatures.Add($celsius)
            }
        }
        if ($temperatures.Count -gt 0) {
            $script:lastTemp = [math]::Round(
                ($temperatures | Measure-Object -Average).Average, 1)
        } else {
            $script:lastTemp = $null
        }
    } catch {
        $script:lastTemp = $null
    }
    return $script:lastTemp
}

function Get-YekProcesses {
    $list = New-Object System.Collections.ArrayList
    try {
        $processes = Get-Process | Sort-Object WorkingSet64 -Descending |
            Select-Object -First 5
        foreach ($process in $processes) {
            try {
                [void]$list.Add([ordered]@{
                    pid = [int]$process.Id
                    name = [string]$process.ProcessName
                    memMb = [math]::Round($process.WorkingSet64 / 1MB, 1)
                })
            } catch {
            }
        }
    } catch {
    }
    return @($list.ToArray())
}

function Get-YekOsName {
    if ($script:OsName) {
        return $script:OsName
    }
    try {
        $os = Get-CimInstance -ClassName Win32_OperatingSystem -ErrorAction Stop
        $script:OsName = ("" + $os.Caption).Trim()
        $script:OsName = $script:OsName -replace '^Microsoft\s+', ''
        if ($os.Version) {
            $script:OsName = "$script:OsName $($os.Version)"
        }
    } catch {
        $script:OsName = [System.Environment]::OSVersion.VersionString
    }
    return $script:OsName
}

function Get-YekStatusJson {
    $cpu = Get-YekCpuPercent
    $memory = Get-YekMemory
    $network = Get-YekNetwork
    $battery = Get-YekBattery
    $temperature = Get-YekTemperature
    $payload = [ordered]@{
        ok = $true
        app = $script:AppName
        version = $script:Version
        name = $script:PcName
        os = Get-YekOsName
        uptimeSec = [int64]([YekBridge.Native]::GetTickCount64() / 1000)
        cpuPercent = $cpu
        ramTotal = $memory.total
        ramUsed = $memory.used
        ramPercent = $memory.percent
        disks = @(Get-YekDisks)
        netDown = $network.down
        netUp = $network.up
        linkMbps = $network.link
        ips = @($network.ips)
        batteryPercent = $battery.percent
        batteryCharging = $battery.charging
        tempC = $temperature
        processes = @(Get-YekProcesses)
        ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    }
    return ($payload | ConvertTo-Json -Compress -Depth 8)
}

# --------------------------------------------------------------- HTTP
function Write-YekResponse {
    param(
        [System.IO.Stream]$Stream,
        [int]$Status,
        [string]$ContentType,
        [string]$Body
    )
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
    $reason = switch ($Status) {
        200 { 'OK' }
        400 { 'Bad Request' }
        401 { 'Unauthorized' }
        404 { 'Not Found' }
        405 { 'Method Not Allowed' }
        429 { 'Too Many Requests' }
        500 { 'Internal Server Error' }
        default { 'OK' }
    }
    $head = "HTTP/1.1 $Status $reason`r`n" +
        "Content-Type: $ContentType`r`n" +
        "Content-Length: $($bytes.Length)`r`n" +
        "Connection: close`r`n" +
        "Cache-Control: no-store`r`n" +
        "X-Content-Type-Options: nosniff`r`n" +
        "Access-Control-Allow-Origin: *`r`n`r`n"
    $headBytes = [System.Text.Encoding]::ASCII.GetBytes($head)
    $Stream.Write($headBytes, 0, $headBytes.Length)
    $Stream.Write($bytes, 0, $bytes.Length)
    $Stream.Flush()
}

function Test-YekAuth {
    param([string]$Remote, [string]$Token)
    $now = Get-Date
    $entry = $script:authFails[$Remote]
    if ($null -ne $entry -and $null -ne $entry.Until -and $now -lt $entry.Until) {
        return 'locked'
    }
    if ($Token -and $Token -ceq $Pin) {
        $script:authFails.Remove($Remote)
        return 'ok'
    }
    $count = 1
    if ($null -ne $entry -and ($now - $entry.At).TotalSeconds -lt 60) {
        $count = $entry.Count + 1
    }
    $until = $null
    if ($count -ge 5) {
        $until = $now.AddMinutes(1)
    }
    $script:authFails[$Remote] = @{ Count = $count; At = $now; Until = $until }
    return 'bad'
}

function Start-YekHttpServer {
    $listener = $null
    try {
        $listener = New-Object System.Net.Sockets.TcpListener(
            [System.Net.IPAddress]::Any, $Port)
        $listener.Start(20)
    } catch {
        Write-Host "HTTP portu $Port açılamadı: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Başka bir köprü çalışıyor olabilir. -Port ile farklı port seçin."
        return
    }

    Write-Host "HTTP dinleniyor : 0.0.0.0:$Port" -ForegroundColor DarkGray
    try {
        while ($true) {
            $client = $null
            try {
                $client = $listener.AcceptTcpClient()
                $client.ReceiveTimeout = 5000
                $client.SendTimeout = 5000
                $remote = $client.Client.RemoteEndPoint.Address.ToString()
                $remote = $remote -replace '^::ffff:', ''
                $stream = $client.GetStream()
                $reader = New-Object System.IO.StreamReader(
                    $stream, [System.Text.Encoding]::ASCII, $false, 4096, $true)
                $requestLine = $reader.ReadLine()
                if ([string]::IsNullOrWhiteSpace($requestLine)) {
                    continue
                }
                $headers = @{}
                while ($true) {
                    $line = $reader.ReadLine()
                    if ($null -eq $line -or $line -eq '') {
                        break
                    }
                    $separator = $line.IndexOf(':')
                    if ($separator -gt 0) {
                        $key = $line.Substring(0, $separator).Trim().ToLowerInvariant()
                        $headers[$key] = $line.Substring($separator + 1).Trim()
                    }
                }
                $parts = $requestLine.Split(' ')
                if ($parts.Length -lt 2) {
                    Write-YekResponse -Stream $stream -Status 400 `
                        -ContentType 'application/json; charset=utf-8' `
                        -Body '{"ok":false,"error":"bad-request"}'
                    continue
                }
                $method = $parts[0].ToUpperInvariant()
                $target = $parts[1]
                $path = $target
                $query = @{}
                if ($target.Contains('?')) {
                    $path = $target.Substring(0, $target.IndexOf('?'))
                    $queryText = $target.Substring($target.IndexOf('?') + 1)
                    foreach ($pair in $queryText.Split('&')) {
                        $equals = $pair.IndexOf('=')
                        if ($equals -gt 0) {
                            $key = [uri]::UnescapeDataString($pair.Substring(0, $equals))
                            $value = [uri]::UnescapeDataString(
                                $pair.Substring($equals + 1))
                            $query[$key] = $value
                        }
                    }
                }

                if ($path -eq '/api/ping') {
                    $body = [ordered]@{
                        ok = $true
                        app = $script:AppName
                        version = $script:Version
                        name = $script:PcName
                        port = $Port
                        protocol = 1
                    } | ConvertTo-Json -Compress
                    Write-YekResponse -Stream $stream -Status 200 `
                        -ContentType 'application/json; charset=utf-8' -Body $body
                    continue
                }

                if ($path -eq '/api/status') {
                    if ($method -ne 'GET') {
                        Write-YekResponse -Stream $stream -Status 405 `
                            -ContentType 'application/json; charset=utf-8' `
                            -Body '{"ok":false,"error":"method"}'
                        continue
                    }
                    $token = ''
                    if ($headers.ContainsKey('x-yek-token')) {
                        $token = $headers['x-yek-token']
                    } elseif ($query.ContainsKey('token')) {
                        $token = $query['token']
                    }
                    $auth = Test-YekAuth -Remote $remote -Token $token
                    if ($auth -eq 'locked') {
                        Write-YekResponse -Stream $stream -Status 429 `
                            -ContentType 'application/json; charset=utf-8' `
                            -Body '{"ok":false,"error":"locked"}'
                        Write-Host ("PIN denemesi kilitlendi: {0}" -f $remote) `
                            -ForegroundColor DarkYellow
                        continue
                    }
                    if ($auth -ne 'ok') {
                        Write-YekResponse -Stream $stream -Status 401 `
                            -ContentType 'application/json; charset=utf-8' `
                            -Body '{"ok":false,"error":"pin"}'
                        Write-Host ("Hatalı PIN: {0}" -f $remote) -ForegroundColor DarkYellow
                        continue
                    }
                    Write-YekResponse -Stream $stream -Status 200 `
                        -ContentType 'application/json; charset=utf-8' `
                        -Body (Get-YekStatusJson)
                    continue
                }

                if ($path -eq '/' -or $path -eq '/index.html') {
                    $html = @"
<!doctype html>
<html lang="tr"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>$($script:AppName)</title>
<style>
body{font-family:system-ui,sans-serif;background:#08111e;color:#e8f1ff;
max-width:620px;margin:40px auto;padding:0 20px;line-height:1.6}
h1{font-size:22px;color:#22d3ee}code{background:#111e31;padding:3px 7px;
border-radius:6px;color:#22d3ee}.muted{color:#8ea4c3}
</style></head><body>
<h1>YEK Panel PC Köprüsü</h1>
<p><b>$($script:PcName)</b> · sürüm $($script:Version)</p>
<p class="muted">Bu adres yalnızca yerel ağda çalışır. Android uygulamasında
PC sekmesine bu bilgisayarın IP adresini ve PIN'i girin.</p>
<p>Durum: <code>GET /api/status</code> · PIN gerekli<br>
Keşif: <code>UDP $DiscoveryPort</code></p>
</body></html>
"@
                    Write-YekResponse -Stream $stream -Status 200 `
                        -ContentType 'text/html; charset=utf-8' -Body $html
                    continue
                }

                Write-YekResponse -Stream $stream -Status 404 `
                    -ContentType 'application/json; charset=utf-8' `
                    -Body '{"ok":false,"error":"not-found"}'
            } catch {
                Write-Host ("İstek hatası: {0}" -f $_.Exception.Message) `
                    -ForegroundColor DarkYellow
            } finally {
                if ($null -ne $client) {
                    try {
                        $client.Close()
                    } catch {
                    }
                }
            }
        }
    } finally {
        try {
            $listener.Stop()
        } catch {
        }
    }
}

# --------------------------------------------------------------- Keşif
function Start-YekDiscoveryJob {
    param([int]$HttpPort, [int]$DiscoveryPort, [string]$Name)
    $job = Start-Job -Name 'yek-panel-discovery' -ScriptBlock {
        param($HttpPort, $DiscoveryPort, $Name, $Version, $AppName)
        try {
            $udp = New-Object System.Net.Sockets.UdpClient($DiscoveryPort)
            $udp.EnableBroadcast = $true
            $udp.Client.ReceiveTimeout = 1500
            $remote = New-Object System.Net.IPEndPoint(
                [System.Net.IPAddress]::Any, 0)
            $payload = [ordered]@{
                ok = $true
                app = $AppName
                version = $Version
                name = $Name
                port = $HttpPort
                protocol = 1
            } | ConvertTo-Json -Compress
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
            while ($true) {
                try {
                    $data = $udp.Receive([ref]$remote)
                    $text = [System.Text.Encoding]::UTF8.GetString($data)
                    if ($text.Trim() -eq 'YEK-PANEL-DISCOVER') {
                        [void]$udp.Send($bytes, $bytes.Length, $remote)
                    }
                } catch [System.Net.Sockets.SocketException] {
                    if ($_.Exception.SocketErrorCode -ne
                        [System.Net.Sockets.SocketError]::TimedOut) {
                        Start-Sleep -Milliseconds 500
                    }
                } catch {
                    Start-Sleep -Milliseconds 500
                }
            }
        } catch {
        } finally {
            try {
                $udp.Close()
            } catch {
            }
        }
    } -ArgumentList $HttpPort, $DiscoveryPort, $Name, $script:Version, $script:AppName
    return $job
}

# --------------------------------------------------------------- Başlangıç
Initialize-YekConfig
$Pin = $script:Pin
$Port = $script:Port

Write-Host ''
Write-Host '=============================================' -ForegroundColor DarkCyan
Write-Host '  YEK PANEL PC KOPRUSU' -ForegroundColor Cyan
Write-Host ("  surum {0} - yerel ag" -f $script:Version) -ForegroundColor DarkGray
Write-Host '=============================================' -ForegroundColor DarkCyan
Write-Host ("  PC      : {0}" -f $script:PcName)
Write-Host ("  Adres   : http://{0}:{1}" -f 'BILGISAYAR-IP', $Port) `
    -ForegroundColor White
Write-Host ("  PIN     : {0}" -f $Pin) -ForegroundColor Yellow
Write-Host ("  Keşif   : UDP {0}" -f $DiscoveryPort) -ForegroundColor DarkGray
Write-Host ("  Ayar    : {0}" -f $script:ConfigPath) -ForegroundColor DarkGray
Write-Host ''
Write-Host '  Telefonda: YEK Panel > PC > BAGLAN' -ForegroundColor Cyan
Write-Host '  Windows guvenlik duvarinda yerel ag izni isteyebilir.' `
    -ForegroundColor DarkGray
Write-Host '  Durdurmak icin Ctrl+C' -ForegroundColor DarkGray
Write-Host ''

# Ilk /api/status yaniti bos kalmasin: ornekleyiciyi isit.
try {
    [void](Get-YekCpuPercent)
    [void](Get-YekNetwork)
    Start-Sleep -Milliseconds 400
    [void](Get-YekCpuPercent)
    [void](Get-YekNetwork)
} catch {
}

$discoveryJob = $null
if (-not $NoDiscovery) {
    Get-Job -Name 'yek-panel-discovery' -ErrorAction SilentlyContinue |
        Stop-Job -ErrorAction SilentlyContinue
    Get-Job -Name 'yek-panel-discovery' -ErrorAction SilentlyContinue |
        Remove-Job -ErrorAction SilentlyContinue
    $discoveryJob = Start-YekDiscoveryJob -HttpPort $Port `
        -DiscoveryPort $DiscoveryPort -Name $PcName
    Write-Host ("Keşif dinleniyor : 0.0.0.0:{0}" -f $DiscoveryPort) `
        -ForegroundColor DarkGray
}

try {
    Start-YekHttpServer
} finally {
    if ($null -ne $discoveryJob) {
        Stop-Job $discoveryJob -ErrorAction SilentlyContinue
        Remove-Job $discoveryJob -Force -ErrorAction SilentlyContinue
    }
}
