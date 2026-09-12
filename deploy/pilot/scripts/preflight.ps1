[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot "..\.env.pilot"),
    [string]$ComposeFile = (Join-Path $PSScriptRoot "..\docker-compose.yml")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "Environment file not found: $EnvFile"
}
if (-not (Test-Path -LiteralPath $ComposeFile)) {
    throw "Compose file not found: $ComposeFile"
}

$values = @{}
Get-Content -LiteralPath $EnvFile -Encoding utf8 | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) {
        return
    }
    $parts = $line -split '=', 2
    if ($parts.Count -eq 2) {
        $values[$parts[0].Trim()] = $parts[1].Trim()
    }
}

function Require-Secret {
    param([string]$Name, [int]$MinimumLength)
    $value = $values[$Name]
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match '(?i)replace|change[_-]?me|example|password') {
        throw "$Name must be replaced with a non-placeholder secret."
    }
    if ($value.Length -lt $MinimumLength) {
        throw "$Name must be at least $MinimumLength characters."
    }
}

foreach ($name in @('MYSQL_DATABASE', 'MYSQL_USER', 'MINIO_ROOT_USER', 'NSO_TLS_CERT_DIR')) {
    if ([string]::IsNullOrWhiteSpace($values[$name])) {
        throw "$name must be set."
    }
}
Require-Secret -Name 'MYSQL_ROOT_PASSWORD' -MinimumLength 16
Require-Secret -Name 'MYSQL_PASSWORD' -MinimumLength 16
Require-Secret -Name 'REDIS_PASSWORD' -MinimumLength 16
Require-Secret -Name 'MINIO_ROOT_PASSWORD' -MinimumLength 16
Require-Secret -Name 'NSO_JWT_SECRET' -MinimumLength 32
foreach ($name in @('NSO_MYSQL_IMAGE','NSO_REDIS_IMAGE','NSO_MINIO_IMAGE','NSO_MINIO_MC_IMAGE')) {
    if ($values[$name] -notmatch '^[^\s]+@sha256:[0-9a-f]{64}$') {
        throw "$name must contain an immutable SHA-256 image digest."
    }
}

$certificateDirectory = $values['NSO_TLS_CERT_DIR']
foreach ($certificate in @('tls.crt', 'tls.key')) {
    if (-not (Test-Path -LiteralPath (Join-Path $certificateDirectory $certificate))) {
        throw "TLS file not found: $(Join-Path $certificateDirectory $certificate)"
    }
}

& (Join-Path $PSScriptRoot 'check-tls.ps1') -CertificatePath (Join-Path $certificateDirectory 'tls.crt')
& docker compose --env-file $EnvFile -f $ComposeFile config --quiet
if ($LASTEXITCODE -ne 0) {
    throw 'Pilot Compose configuration validation failed.'
}
Write-Host 'Pilot preflight passed.'
