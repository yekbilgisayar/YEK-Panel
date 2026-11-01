Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$logoPath = Join-Path $root 'YekPanel\Assets\logo.png'
$outPath = Join-Path $PSScriptRoot 'wizard.bmp'

$bmp = New-Object System.Drawing.Bitmap 164, 314
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$rect = New-Object System.Drawing.Rectangle 0, 0, 164, 314
$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $rect,
    [System.Drawing.Color]::FromArgb(255, 8, 15, 28),
    [System.Drawing.Color]::FromArgb(255, 17, 36, 61),
    90.0)
$g.FillRectangle($brush, $rect)
$brush.Dispose()

$accent = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 56, 189, 248))
$g.FillRectangle($accent, 0, 0, 164, 4)
$accent.Dispose()

$card = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 14, 27, 45))
$border = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 30, 58, 95), 1)
$g.FillRectangle($card, 32, 26, 100, 96)
$g.DrawRectangle($border, 32, 26, 100, 96)
$card.Dispose()
$border.Dispose()

$logo = [System.Drawing.Image]::FromFile($logoPath)
$g.DrawImage($logo, 42, 34, 80, 80)
$logo.Dispose()

$white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 241, 245, 249))
$muted = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 148, 163, 184))
$sky = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 56, 189, 248))

$fontBig = New-Object System.Drawing.Font('Segoe UI', 25, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fontMid = New-Object System.Drawing.Font('Segoe UI', 16, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fontSmall = New-Object System.Drawing.Font('Segoe UI', 11, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)

$format = New-Object System.Drawing.StringFormat
$format.Alignment = [System.Drawing.StringAlignment]::Center

$g.DrawString('YEK', $fontBig, $white, (New-Object System.Drawing.RectangleF 0, 132, 164, 40), $format)
$g.DrawString('PANEL', $fontMid, $sky, (New-Object System.Drawing.RectangleF 0, 172, 164, 28), $format)
$g.DrawString('v2.0', $fontSmall, $muted, (New-Object System.Drawing.RectangleF 0, 204, 164, 20), $format)
$g.DrawString('SİSTEM & SUNUCU', $fontSmall, $muted, (New-Object System.Drawing.RectangleF 0, 258, 164, 20), $format)
$g.DrawString('İZLEME PANELİ', $fontSmall, $muted, (New-Object System.Drawing.RectangleF 0, 274, 164, 20), $format)

$fontBig.Dispose()
$fontMid.Dispose()
$fontSmall.Dispose()
$white.Dispose()
$muted.Dispose()
$sky.Dispose()
$format.Dispose()
$g.Dispose()

$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Bmp)
$bmp.Dispose()

Write-Output ('Wizard image: ' + $outPath)
