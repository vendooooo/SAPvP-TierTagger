param(
    [string]$SourceDir = (Join-Path $PSScriptRoot '..\design\sa-icons-png'),
    [string]$OutputDir = (Join-Path $PSScriptRoot '..\src\main\resources\assets\sapvptiertagger\textures\font'),
    [int]$CanvasSize = 48,
    [int]$Padding = 4
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$icons = @(
    'netherite_pot.png',
    'sword.png',
    'axe.png',
    'smp.png',
    'mace.png',
    'crystal.png'
)

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
        throw 'Icone sem pixels visiveis.'
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

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

foreach ($icon in $icons) {
    $sourcePath = Join-Path $SourceDir $icon
    if (-not (Test-Path $sourcePath)) {
        throw "PNG nao encontrado: $sourcePath"
    }

    $raw = [System.Drawing.Bitmap]::FromFile($sourcePath)
    try {
        $bounds = Get-AlphaBounds $raw
        $cropped = $raw.Clone($bounds, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            Convert-ToWhite $cropped
            $canvas = New-IconCanvas -source $cropped -canvasSize $CanvasSize -padding $Padding
            try {
                $targetPath = Join-Path $OutputDir $icon
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
        $raw.Dispose()
    }
}
