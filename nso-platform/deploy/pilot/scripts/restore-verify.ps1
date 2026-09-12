[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BackupDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$sqlFile = Join-Path $BackupDirectory 'mysql.sql'
$objectFile = Join-Path $BackupDirectory 'minio-data.tar.gz'
$manifestFile = Join-Path $BackupDirectory 'SHA256SUMS.txt'
foreach ($requiredFile in @($sqlFile, $objectFile, $manifestFile)) {
    if (-not (Test-Path -LiteralPath $requiredFile)) {
        throw "Backup artifact not found: $requiredFile"
    }
}

$expected = Get-Content -LiteralPath $manifestFile -Encoding utf8
foreach ($line in $expected) {
    $parts = $line -split '\s{2,}', 2
    if ($parts.Count -ne 2) {
        throw "Invalid checksum line: $line"
    }
    $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $BackupDirectory $parts[1])).Hash.ToLowerInvariant()
    if ($actual -ne $parts[0].ToLowerInvariant()) {
        throw "Checksum verification failed for $($parts[1])"
    }
}

$containerName = "nso-restore-verify-" + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$restorePassword = [Guid]::NewGuid().ToString('N')
try {
    & docker run --detach --name $containerName -e "MYSQL_ROOT_PASSWORD=$restorePassword" -e MYSQL_DATABASE=nso_restore_verify mysql:8.4.4 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Unable to start the disposable MySQL restore container.' }

    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        & docker exec $containerName mysqladmin ping -h 127.0.0.1 -uroot "-p$restorePassword" --silent | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'Disposable MySQL container did not become ready.' }

    & docker cp $sqlFile "${containerName}:/tmp/mysql.sql"
    if ($LASTEXITCODE -ne 0) { throw 'Unable to copy MySQL backup into the restore container.' }
    & docker exec $containerName sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" nso_restore_verify < /tmp/mysql.sql'
    if ($LASTEXITCODE -ne 0) { throw 'MySQL restore verification failed.' }
    & docker exec $containerName sh -c 'mysql -N -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1" nso_restore_verify'
    if ($LASTEXITCODE -ne 0) { throw 'Restored database cannot query Flyway history.' }

    & docker run --rm -v "${BackupDirectory}:/backup:ro" alpine:3.20 tar -tzf /backup/minio-data.tar.gz | Select-Object -First 1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'MinIO archive verification failed.' }
    Write-Host 'Backup restore verification completed in an isolated MySQL container.'
}
finally {
    & docker rm -f $containerName 2>$null | Out-Null
}
