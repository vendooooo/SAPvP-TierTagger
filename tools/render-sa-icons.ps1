param(
    [string]$SourceDir = (Join-Path $PSScriptRoot '..\design\sa-icons'),
    [string]$OutputDir = (Join-Path $PSScriptRoot '..\src\main\resources\assets\sapvptiertagger\textures\font'),
    [int]$CanvasSize = 32,
    [int]$WindowSize = 256,
    [int]$SvgSize = 208,
    [int]$Padding = 2
)

$ErrorActionPreference = 'Stop'

$chromeCandidates = @(
    'C:\Program Files\Google\Chrome\Application\chrome.exe',
    'C:\Program Files (x86)\Google\Chrome\Application\chrome.exe',
    'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
)
$browser = $chromeCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $browser) {
    throw 'Nenhum Chrome/Edge headless encontrado para rasterizar os SVGs.'
}

$icons = @(
    @{ Input = 'netherite_pot.svg'; Output = 'netherite_pot.png' },
    @{ Input = 'sword.svg'; Output = 'sword.png' },
    @{ Input = 'axe.svg'; Output = 'axe.png' },
    @{ Input = 'smp.svg'; Output = 'smp.png' },
    @{ Input = 'mace.svg'; Output = 'mace.png' },
    @{ Input = 'crystal.svg'; Output = 'crystal.png' }
)

Add-Type -AssemblyName System.Drawing

function Get-AlphaBounds([System.Drawing.Bitmap]$bitmap) {
    $minX = $bitmap.Width
    $minY = $bitmap.Height
    $maxX = -1
    $maxY = -1

    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            $pixel = $bitmap.GetPixel($x, $y)
            if ($pixel.A -gt 0) {
                if ($x -lt $minX) { $minX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }

    if ($maxX -lt 0 -or $maxY -lt 0) {
        throw 'SVG rasterizado sem pixels visiveis.'
    }

    return [System.Drawing.Rectangle]::FromLTRB($minX, $minY, $maxX + 1, $maxY + 1)
}

function Convert-ToWhite([System.Drawing.Bitmap]$bitmap) {
    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            $pixel = $bitmap.GetPixel($x, $y)
            if ($pixel.A -gt 0) {
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, 255, 255, 255))
            }
        }
    }
}

function New-IconCanvas([System.Drawing.Bitmap]$source, [int]$canvasSize, [int]$padding) {
    $canvas = New-Object System.Drawing.Bitmap($canvasSize, $canvasSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($canvas)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

        $innerSize = $canvasSize - ($padding * 2)
        $scale = [Math]::Min($innerSize / [double]$source.Width, $innerSize / [double]$source.Height)
        $drawWidth = [Math]::Max(1, [int][Math]::Round($source.Width * $scale))
        $drawHeight = [Math]::Max(1, [int][Math]::Round($source.Height * $scale))
        $drawX = [int][Math]::Floor(($canvasSize - $drawWidth) / 2)
        $drawY = [int][Math]::Floor(($canvasSize - $drawHeight) / 2)

        $graphics.DrawImage($source, (New-Object System.Drawing.Rectangle($drawX, $drawY, $drawWidth, $drawHeight)))
    }
    finally {
        $graphics.Dispose()
    }

    return $canvas
}

$tempDir = Join-Path $env:TEMP 'sapvptiertagger-icon-render'
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

foreach ($icon in $icons) {
    $svgPath = Join-Path $SourceDir $icon.Input
    if (-not (Test-Path $svgPath)) {
        throw "SVG nao encontrado: $svgPath"
    }

    $svgMarkup = Get-Content $svgPath -Raw
    $html = @"
<!doctype html>
<html>
<head>
<meta charset="utf-8" />
<style>
html, body {
  margin: 0;
  width: ${WindowSize}px;
  height: ${WindowSize}px;
  background: transparent;
  overflow: hidden;
}
body {
  display: flex;
  align-items: center;
  justify-content: center;
}
svg {
  width: ${SvgSize}px !important;
  height: ${SvgSize}px !important;
  display: block;
}
svg * {
  fill: white !important;
  stroke: white !important;
}
</style>
</head>
<body>
$svgMarkup
</body>
</html>
"@

    $htmlPath = Join-Path $tempDir ([System.IO.Path]::GetFileNameWithoutExtension($icon.Input) + '.html')
    $shotPath = Join-Path $tempDir ([System.IO.Path]::GetFileNameWithoutExtension($icon.Input) + '.png')
    Set-Content -Path $htmlPath -Value $html -Encoding UTF8

    & $browser --headless --disable-gpu --window-size=$WindowSize,$WindowSize --default-background-color=00000000 --screenshot="$shotPath" "file:///$($htmlPath -replace '\\','/')" | Out-Null
    if (-not (Test-Path $shotPath)) {
        throw "Falha ao rasterizar $svgPath"
    }

    $rawBitmap = [System.Drawing.Bitmap]::FromFile($shotPath)
    try {
        $bounds = Get-AlphaBounds $rawBitmap
        $cropped = $rawBitmap.Clone($bounds, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            Convert-ToWhite $cropped
            $canvas = New-IconCanvas -source $cropped -canvasSize $CanvasSize -padding $Padding
            try {
                $targetPath = Join-Path $OutputDir $icon.Output
                $canvas.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
                Write-Output "Gerado $targetPath"
            }
            finally {
                $canvas.Dispose()
            }
        }
        finally {
            $cropped.Dispose()
        }
    }
    finally {
        $rawBitmap.Dispose()
    }
}
