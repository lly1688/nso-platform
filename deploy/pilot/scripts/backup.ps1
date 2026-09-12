[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BackupRoot,
    [string]$ComposeFile = (Join-Path $PSScriptRoot "..\docker-compose.yml"),
    [string]$EnvFile = (Join-Path $PSScriptRoot "..\.env.pilot")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $ComposeFile)) {
    throw "Compose file not found: $ComposeFile"
}
if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "Environment file not found: $EnvFile"
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupDirectory = Join-Path $BackupRoot $timestamp
New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null

$sqlFile = Join-Path $backupDirectory 'mysql.sql'
$objectFile = Join-Path $backupDirectory 'minio-data.tar.gz'
$manifestFile = Join-Path $backupDirectory 'SHA256SUMS.txt'
$composeArgs = @('--env-file', $EnvFile, '-f', $ComposeFile)

& docker compose @composeArgs exec -T mysql sh -c 'exec mysqldump --single-transaction --routines --events -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > $sqlFile
if ($LASTEXITCODE -ne 0) {
    throw 'MySQL backup failed.'
}

& docker compose @composeArgs exec -T minio sh -c 'tar -C /data -czf - .' > $objectFile
if ($LASTEXITCODE -ne 0) {
    throw 'MinIO backup failed.'
}

Get-FileHash -Algorithm SHA256 -LiteralPath $sqlFile, $objectFile |
    ForEach-Object { "{0}  {1}" -f $_.Hash.ToLowerInvariant(), $_.Path.Substring($backupDirectory.Length + 1) } |
    Set-Content -LiteralPath $manifestFile -Encoding utf8

Write-Host "Backup completed: $backupDirectory"
