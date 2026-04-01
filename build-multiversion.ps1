param(
	[string[]]$Profiles = @("1.21.10", "1.21.11")
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$outputDir = Join-Path $root "dist\\multiversion"

if (Test-Path $outputDir -PathType Leaf) {
	Remove-Item $outputDir -Force
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

foreach ($profile in $Profiles) {
	Write-Host "==> Building SAPVPTierTagger for Minecraft $profile" -ForegroundColor Cyan
	& "$root\\gradlew.bat" clean build "--console=plain" "--no-daemon" "-PmcProfile=$profile"
	if ($LASTEXITCODE -ne 0) {
		throw "Gradle build failed for profile $profile"
	}

	Get-ChildItem (Join-Path $root "build\\libs\\*.jar") |
		Where-Object { $_.Name -notmatch "-sources\\.jar$" } |
		ForEach-Object {
			Copy-Item $_.FullName -Destination (Join-Path $outputDir $_.Name) -Force
		}
}

Write-Host ""
Write-Host "Builds available in $outputDir" -ForegroundColor Green
