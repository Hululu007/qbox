# QBox branding generator (2026-11)
# 用法: powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\make-logo.ps1
# 输入: branding\qbox-logo.png（透明底、正方形）
# 输出: 应用内 logo / 桌面图标 / leanback banner / Play 商店图，全部由同一张原图缩放。
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Join-Path $PSScriptRoot '..'
$srcPath = Join-Path $root 'branding\qbox-logo.png'
$res = Join-Path $root 'fongmi-561\app\src'

if (-not (Test-Path $srcPath)) { throw "missing source: $srcPath" }
$src = [System.Drawing.Bitmap]::new($srcPath)

# 1) 计算 alpha 包围盒，裁掉原图四周留白
$minX = $src.Width; $minY = $src.Height; $maxX = -1; $maxY = -1
for ($y = 0; $y -lt $src.Height; $y++) {
    for ($x = 0; $x -lt $src.Width; $x++) {
        if ($src.GetPixel($x, $y).A -gt 8) {
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
if ($maxX -lt 0) { throw 'source image is fully transparent' }
$crop = [System.Drawing.Rectangle]::new($minX, $minY, $maxX - $minX + 1, $maxY - $minY + 1)
Write-Host "source $($src.Width)x$($src.Height), glyph $($crop.Width)x$($crop.Height)"

function New-Canvas([int]$size, [int]$glyphPercent, $bgColor) {
    $bmp = [System.Drawing.Bitmap]::new($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    if ($null -ne $bgColor) { $g.Clear($bgColor) } else { $g.Clear([System.Drawing.Color]::Transparent) }
    $target = [int]($size * $glyphPercent / 100.0)
    # 保持原图比例，长边贴合 target
    $ratio = [Math]::Min($target / $crop.Width, $target / $crop.Height)
    $w = [int][Math]::Round($crop.Width * $ratio)
    $h = [int][Math]::Round($crop.Height * $ratio)
    $dx = [int](($size - $w) / 2)
    $dy = [int](($size - $h) / 2)
    $g.DrawImage($src, [System.Drawing.Rectangle]::new($dx, $dy, $w, $h), $crop, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    return $bmp
}

function New-Round([int]$size, [int]$glyphPercent, $bgColor) {
    $bmp = [System.Drawing.Bitmap]::new($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $bg = [System.Drawing.SolidBrush]::new($bgColor)
    $g.FillEllipse($bg, 0, 0, $size - 1, $size - 1)
    $bg.Dispose()
    # 圆形安全区：内部 78%
    $target = [int]($size * $glyphPercent / 100.0)
    $ratio = [Math]::Min($target / $crop.Width, $target / $crop.Height)
    $w = [int][Math]::Round($crop.Width * $ratio)
    $h = [int][Math]::Round($crop.Height * $ratio)
    $dx = [int](($size - $w) / 2)
    $dy = [int](($size - $h) / 2)
    $g.DrawImage($src, [System.Drawing.Rectangle]::new($dx, $dy, $w, $h), $crop, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    return $bmp
}

function New-Banner([int]$w, [int]$h) {
    $bmp = [System.Drawing.Bitmap]::new($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    # 电视桌面按 banner 显示磁贴：保持和图标一致的「白底 + 纯 logo」，不要文字。
    $g.Clear([System.Drawing.Color]::White)
    $glyph = [int]($h * 0.76)
    $ratio = [Math]::Min($glyph / $crop.Width, $glyph / $crop.Height)
    $lw = [int][Math]::Round($crop.Width * $ratio)
    $lh = [int][Math]::Round($crop.Height * $ratio)
    $lx = [int](($w - $lw) / 2)
    $ly = [int](($h - $lh) / 2)
    $g.DrawImage($src, [System.Drawing.Rectangle]::new($lx, $ly, $lw, $lh), $crop, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    return $bmp
}

$white = [System.Drawing.Color]::White
$jpeg = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$jpegParams = [System.Drawing.Imaging.EncoderParameters]::new(1)
$jpegParams.Param[0] = [System.Drawing.Imaging.EncoderParameter]::new([System.Drawing.Imaging.Encoder]::Quality, [long]92)

function Save-Png($bmp, $path) {
    $dir = Split-Path $path -Parent
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "  $path"
}

function Save-Jpg($bmp, $path) {
    $dir = Split-Path $path -Parent
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $bmp.Save($path, $jpeg, $jpegParams)
    $bmp.Dispose()
    Write-Host "  $path"
}

Write-Host 'QBox branding:'
# 应用内左上角 logo：透明底，比原来 192 大一档，保证 4K 不糊
Save-Png (New-Canvas 256 94 $null) (Join-Path $res 'main\res\drawable-nodpi\ic_logo.png')
# 自适应图标前景：白色不透明底 + 62% glyph。
# 有些电视桌面不认 adaptive-icon 的 <background> 颜色，会拿默认蓝色底兜底，
# 前景自带白底后不管桌面怎么渲染都是干净的白磁贴。
Save-Png (New-Canvas 512 62 $white) (Join-Path $res 'main\res\drawable-nodpi\ic_launcher_foreground.png')
# 传统图标：白底，glyph 84%
$iconSizes = [ordered]@{ 'mdpi' = 48; 'hdpi' = 72; 'xhdpi' = 96; 'xxhdpi' = 144; 'xxxhdpi' = 192 }
foreach ($k in $iconSizes.Keys) {
    $s = $iconSizes[$k]
    Save-Png (New-Canvas $s 84 $white) (Join-Path $res "main\res\mipmap-$k\ic_launcher.png")
    Save-Png (New-Round $s 68 $white) (Join-Path $res "main\res\mipmap-$k\ic_launcher_round.png")
}
Save-Png (New-Canvas 512 84 $white) (Join-Path $res 'main\ic_launcher-playstore.png')
# leanback banner：320x180 dp，xhdpi 出 640x360
Save-Png (New-Banner 640 360) (Join-Path $res 'leanback\res\drawable-xhdpi\ic_banner.png')

$src.Dispose()
Write-Host 'done'
