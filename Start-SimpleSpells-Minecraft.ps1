$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSCommandPath
Set-Location -LiteralPath $projectRoot

Write-Host ''
Write-Host 'Starting Minecraft with Simple Spells loaded...'
Write-Host "Project folder: $projectRoot"
Write-Host ''
Write-Host 'This runs the NeoForge Gradle dev client, so it loads the mod directly from this project.'
Write-Host ''

& '.\gradlew.bat' '--gradle-user-home' '.gradle-user-home' 'runClient'
$exitCode = $LASTEXITCODE

Write-Host ''
if ($exitCode -eq 0) {
    Write-Host 'Minecraft closed.'
} else {
    Write-Host "Minecraft or Gradle exited with code $exitCode."
}

Read-Host 'Press Enter to close this window'
exit $exitCode
