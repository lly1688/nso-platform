[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BaseUrl
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$base = $BaseUrl.TrimEnd('/')

$health = Invoke-WebRequest -Uri "$base/healthz" -UseBasicParsing
if ($health.StatusCode -ne 200) {
    throw "Gateway health check returned $($health.StatusCode)."
}

try {
    Invoke-WebRequest -Uri "$base/api/v1/admin/auth/me" -UseBasicParsing | Out-Null
    throw 'Unauthenticated auth/me request unexpectedly succeeded.'
}
catch {
    $response = $_.Exception.Response
    if ($null -eq $response -or [int]$response.StatusCode -ne 401) {
        throw
    }
    if ([string]::IsNullOrWhiteSpace($response.Headers['X-Trace-Id'])) {
        throw 'Unauthenticated response did not include X-Trace-Id.'
    }
}

Write-Host 'Pilot gateway and unauthenticated security smoke checks passed.'
