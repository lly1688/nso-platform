[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$migrationDirectory = Join-Path $repoRoot 'nso-admin\src\main\resources\db\migration'
$legacyDirectory = Join-Path $repoRoot 'sql'

if (-not (Test-Path -LiteralPath $migrationDirectory)) {
    throw "Canonical Flyway directory not found: $migrationDirectory"
}

$legacyScripts = Get-ChildItem -LiteralPath $legacyDirectory -File -Filter 'V*__*.sql' -ErrorAction SilentlyContinue
if ($legacyScripts) {
    throw "Root sql directory contains executable-looking Flyway scripts. Move them to sql/archive: $($legacyScripts.Name -join ', ')"
}

$migrations = Get-ChildItem -LiteralPath $migrationDirectory -File -Filter 'V*__*.sql' |
    ForEach-Object {
        if ($_.Name -notmatch '^V(\d+)__.+\.sql$') {
            throw "Invalid Flyway filename: $($_.Name)"
        }
        [PSCustomObject]@{ Version = [int]$Matches[1]; Name = $_.Name }
    } |
    Sort-Object Version

if (-not $migrations) {
    throw 'No canonical Flyway migrations found.'
}

$duplicates = $migrations | Group-Object Version | Where-Object Count -gt 1
if ($duplicates) {
    throw "Duplicate Flyway versions: $($duplicates.Name -join ', ')"
}

$expected = 1
foreach ($migration in $migrations) {
    if ($migration.Version -ne $expected) {
        throw "Flyway version sequence has a gap before V$($migration.Version)."
    }
    $expected++
}
if ($migrations[-1].Version -lt 31) {
    throw 'The pilot baseline must include the published V1-V31 migration history.'
}

Write-Host "Canonical Flyway history verified through V$($migrations[-1].Version). New schema changes must use V$expected or later."
