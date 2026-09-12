[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$CertificatePath,
    [ValidateRange(1,3650)][int]$WarnDays = 30,
    [string]$MetricPath
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $CertificatePath)) { throw "Certificate not found: $CertificatePath" }
$cert = [System.Security.Cryptography.X509Certificates.X509Certificate2]::new((Resolve-Path -LiteralPath $CertificatePath).Path)
try {
    $now = [DateTime]::UtcNow
    $expiry = [DateTimeOffset]::new($cert.NotAfter.ToUniversalTime()).ToUnixTimeSeconds()
    if ($MetricPath) {
        $temporary = "$MetricPath.tmp"
        "nso_tls_not_after_seconds $expiry`nnso_tls_check_timestamp_seconds $([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())`n" | Set-Content -LiteralPath $temporary -Encoding ascii
        Move-Item -LiteralPath $temporary -Destination $MetricPath -Force
    }
    if ($cert.NotBefore.ToUniversalTime() -gt $now) { throw 'TLS certificate is not valid yet.' }
    $remaining = ($cert.NotAfter.ToUniversalTime() - $now).TotalDays
    if ($remaining -lt $WarnDays) { throw "TLS certificate expires in $([math]::Round($remaining, 1)) days; renew before $($cert.NotAfter.ToString('o'))." }
    Write-Host "TLS certificate valid until $($cert.NotAfter.ToString('o'))"
}
finally { $cert.Dispose() }
