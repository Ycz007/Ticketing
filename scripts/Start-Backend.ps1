param([string]$EnvFile = (Join-Path $PSScriptRoot '../.env'))
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $EnvFile)) { throw 'Copy .env.example to .env and set local credentials first.' }
foreach ($line in Get-Content -LiteralPath $EnvFile) {
    if ($line -match '^\s*(#|$)') { continue }
    if ($line -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') { throw 'Invalid .env syntax; use NAME=value without shell expressions.' }
    $name = $Matches[1]
    $value = $Matches[2]
    if ($name -notin @('DB_URL','DB_USERNAME','DB_PASSWORD','MINIO_ENDPOINT','MINIO_ACCESS_KEY','MINIO_SECRET_KEY','MINIO_BUCKET','DEMO_ENABLED','DEMO_PASSWORD','SERVER_PORT','SESSION_COOKIE_SECURE','MYSQL_ROOT_PASSWORD')) {
        throw "Unsupported setting: $name"
    }
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
Push-Location (Join-Path $PSScriptRoot '../backend')
try { & mvn.cmd spring-boot:run; if ($LASTEXITCODE -ne 0) { throw 'Backend exited with an error.' } }
finally { Pop-Location }
