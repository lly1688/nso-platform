[CmdletBinding()]
param(
    [switch]$InstallFrontendDependencies,
    [string]$BaseUrl
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    & (Join-Path $PSScriptRoot 'verify-migrations.ps1')

    & mvn test
    if ($LASTEXITCODE -ne 0) { throw 'Backend test suite failed.' }

    Push-Location (Join-Path $repoRoot 'nso-web')
    try {
        if ($InstallFrontendDependencies -or -not (Test-Path -LiteralPath 'node_modules')) {
            & npm ci
            if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
        }
        & npm run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend production build failed.' }
    }
    finally {
        Pop-Location
    }

    & docker compose --env-file deploy/pilot/.env.example -f deploy/pilot/docker-compose.yml config | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Pilot Compose configuration validation failed.' }

    if (-not [string]::IsNullOrWhiteSpace($BaseUrl)) {
        & (Join-Path $PSScriptRoot 'smoke-pilot.ps1') -BaseUrl $BaseUrl
    }

    Write-Host 'Pilot verification completed.'
}
finally {
    Pop-Location
}
