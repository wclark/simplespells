
# Simple Spells

Simple Spells is a NeoForge mod for Minecraft 1.21.1.

## Versions

- Minecraft: 1.21.1
- NeoForge: 21.1.219
- Java: 21
- Gradle wrapper: 9.5.0

## Development

Import this folder as a Gradle project in Eclipse or IntelliJ IDEA.

Useful commands:

```powershell
.\gradlew.bat build
.\gradlew.bat check
.\gradlew.bat runClient
```

The built mod jar appears in `build/libs`.

`check` runs compilation, Checkstyle, unit tests, and a JaCoCo coverage report. Reports are written to:

- `build/reports/checkstyle/main.html`
- `build/reports/tests/test/index.html`
- `build/reports/jacoco/test/html/index.html`

## Releases

GitHub Releases are built from version tags. To publish a release, update `mod_version` in `gradle.properties`, commit the change, then push a tag such as `v1.0.0`.

The release workflow builds the project on GitHub and attaches `simplespells.jar` to the release. Players can download that jar and place it in their Minecraft instance's `mods` folder.

## Optional Local Launcher Scripts

The double-click scripts are local helpers and are intentionally not committed. If you want them, create these two files in the project folder.

`Start-SimpleSpells-Minecraft.cmd`:

```bat
@echo off
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Start-SimpleSpells-Minecraft.ps1"
```

`Start-SimpleSpells-Minecraft.ps1`:

```powershell
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
```

## Notes

This project started from the official NeoForge MDK for Minecraft 1.21.1 and was pinned to match the local CurseForge NeoForge install at `C:\Users\bill\curseforge\minecraft\Install\versions\neoforge-21.1.219`.
