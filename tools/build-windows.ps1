param([string[]]$GradleArgs = @(':app:assembleDebug'))
$ErrorActionPreference = 'Stop'
$Project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Gradle = Get-Command gradle -ErrorAction SilentlyContinue
if ($Gradle) { & $Gradle.Source @GradleArgs; exit $LASTEXITCODE }
$Version = '8.11.1'
$HomeDir = Join-Path $Project '.gradle-local'
$Program = Join-Path $HomeDir "gradle-$Version\bin\gradle.bat"
if (!(Test-Path $Program)) {
  New-Item -ItemType Directory -Force -Path $HomeDir | Out-Null
  $Archive = Join-Path $HomeDir "gradle-$Version-bin.zip"
  Write-Host "Downloading Gradle $Version (first build requires internet)..."
  Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$Version-bin.zip" -OutFile $Archive
  Expand-Archive -Path $Archive -DestinationPath $HomeDir -Force
  Remove-Item $Archive
}
Push-Location $Project
try { & $Program @GradleArgs; exit $LASTEXITCODE }
finally { Pop-Location }
