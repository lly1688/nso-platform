[CmdletBinding()]
param(
    [string]$Output = (Join-Path (Split-Path -Parent $PSScriptRoot) 'deploy\pilot\release-evidence.json'),
    [string]$BaseUrl
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    $migrationPath = Join-Path $repoRoot 'nso-admin\src\main\resources\db\migration'
    $versions = Get-ChildItem $migrationPath -Filter '*.sql' | ForEach-Object {
        if ($_.Name -match '^V(\d+)__') { [int]$Matches[1] }
    } | Sort-Object
    $health = $null
    if ($BaseUrl) {
        try { $health = Invoke-RestMethod -Uri ($BaseUrl.TrimEnd('/') + '/healthz') -TimeoutSec 10 } catch { $health = @{ error = $_.Exception.Message } }
    }
    $evidence = [ordered]@{
        generatedAt = [DateTime]::UtcNow.ToString('o')
        gitCommit = (& git rev-parse HEAD).Trim()
        branch = (& git branch --show-current).Trim()
        migrationCount = @($versions).Count
        migrationVersion = if (@($versions).Count) { @($versions)[-1] } else { $null }
        frontendBuild = 'npm run build is required by verify-pilot.ps1'
        backendTests = 'mvn test is required by verify-pilot.ps1'
        healthz = $health
        knownLimitations = @('浏览器端到端验收需在真实试点环境执行', '镜像漏洞扫描需接入运营方扫描器')
    }
    $parent = Split-Path -Parent $Output
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    $evidence | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $Output -Encoding utf8
    Write-Host "Release evidence written to $Output"
}
finally { Pop-Location }
