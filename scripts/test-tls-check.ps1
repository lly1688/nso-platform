$ErrorActionPreference = 'Stop'
$check = Join-Path $PSScriptRoot '../deploy/pilot/scripts/check-tls.ps1'
$directory = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($directory) | Out-Null
$key = [Security.Cryptography.RSA]::Create(2048)
try {
    foreach ($days in @(90, 2, -1)) {
        $request = [Security.Cryptography.X509Certificates.CertificateRequest]::new('CN=localhost', $key, [Security.Cryptography.HashAlgorithmName]::SHA256, [Security.Cryptography.RSASignaturePadding]::Pkcs1)
        $cert = $request.CreateSelfSigned([DateTimeOffset]::UtcNow.AddDays(-5), [DateTimeOffset]::UtcNow.AddDays($days))
        $file = Join-Path $directory "$days.crt"
        [IO.File]::WriteAllText($file, $cert.ExportCertificatePem())
        $rejected = $false
        try { & $check -CertificatePath $file -MetricPath (Join-Path $directory 'tls.prom') } catch { $rejected = $true }
        if ($rejected -ne ($days -lt 30)) { throw "Incorrect expiry decision for $days days." }
        $cert.Dispose()
    }
    $rejected = $false
    try { & $check -CertificatePath (Join-Path $directory 'missing.crt') } catch { $rejected = $true }
    if (-not $rejected) { throw 'Missing certificate was accepted.' }
    Write-Host 'TLS checks passed: valid, expiring, expired, missing.'
}
finally { $key.Dispose() }
