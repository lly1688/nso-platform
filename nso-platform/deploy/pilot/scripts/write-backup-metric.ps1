[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BackupRoot,
    [Parameter(Mandatory)]
    [string]$TextfileDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$latest = Get-ChildItem -LiteralPath $BackupRoot -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'SHA256SUMS.txt') } |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1
if ($null -eq $latest) {
    throw "No completed backup directory found under $BackupRoot"
}

$ageHours = [Math]::Round(((Get-Date).ToUniversalTime() - $latest.LastWriteTimeUtc).TotalHours, 3)
New-Item -ItemType Directory -Force -Path $TextfileDirectory | Out-Null
@(
    '# HELP nso_backup_age_hours Age of the latest verified NSO backup.'
    '# TYPE nso_backup_age_hours gauge'
    "nso_backup_age_hours $ageHours"
) | Set-Content -LiteralPath (Join-Path $TextfileDirectory 'nso_backup.prom') -Encoding ascii

Write-Host "Backup metric written for $($latest.FullName)."
