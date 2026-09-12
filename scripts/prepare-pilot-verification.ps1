[CmdletBinding()]
param([string]$OutputDirectory = (Join-Path ([IO.Path]::GetTempPath()) 'nso-pilot-verify'))
$ErrorActionPreference = 'Stop'
[IO.Directory]::CreateDirectory((Join-Path $OutputDirectory 'certs')) | Out-Null
$key = [Security.Cryptography.RSA]::Create(2048)
try {
    $request = [Security.Cryptography.X509Certificates.CertificateRequest]::new('CN=localhost', $key, [Security.Cryptography.HashAlgorithmName]::SHA256, [Security.Cryptography.RSASignaturePadding]::Pkcs1)
    $san = [Security.Cryptography.X509Certificates.SubjectAlternativeNameBuilder]::new()
    $san.AddDnsName('localhost')
    $san.AddIpAddress([Net.IPAddress]::Loopback)
    $request.CertificateExtensions.Add($san.Build())
    $cert = $request.CreateSelfSigned([DateTimeOffset]::UtcNow.AddMinutes(-5), [DateTimeOffset]::UtcNow.AddDays(45))
    [IO.File]::WriteAllText((Join-Path $OutputDirectory 'certs/tls.crt'), $cert.ExportCertificatePem())
    [IO.File]::WriteAllText((Join-Path $OutputDirectory 'certs/tls.key'), $key.ExportPkcs8PrivateKeyPem())
    $cert.Dispose()
}
finally { $key.Dispose() }
$values = Get-Content (Join-Path $PSScriptRoot '../deploy/pilot/.env.example') | ForEach-Object {
    if ($_ -match '^(MYSQL_ROOT_PASSWORD|MYSQL_PASSWORD|REDIS_PASSWORD|MINIO_ROOT_PASSWORD|NSO_JWT_SECRET)=') {
        $name = ($_ -split '=', 2)[0]
        "$name=$([Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)))"
    } elseif ($_ -match '^NSO_TLS_CERT_DIR=') { 'NSO_TLS_CERT_DIR=' + (Join-Path $OutputDirectory 'certs').Replace('\', '/') }
    elseif ($_ -match '^NSO_HTTP_PORT=') { 'NSO_HTTP_PORT=8088' }
    elseif ($_ -match '^NSO_HTTPS_PORT=') { 'NSO_HTTPS_PORT=8443' }
    else { $_ }
}
$values | Set-Content -LiteralPath (Join-Path $OutputDirectory '.env.pilot') -Encoding utf8
Write-Host "Isolated verification configuration prepared in $OutputDirectory. Self-signed certificate; no OS trust changes."
