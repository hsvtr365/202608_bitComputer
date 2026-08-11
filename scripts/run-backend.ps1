$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'

if (-not (Test-Path $envFile)) {
    throw '.env가 없습니다. .env.example을 참고하세요.'
}

Get-Content $envFile | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object {
    $pair = $_.Split('=', 2)
    if ($pair.Count -eq 2) { Set-Item -Path "Env:$($pair[0])" -Value $pair[1] }
}

$jdk = Get-ChildItem (Join-Path $projectRoot '.tools') -Directory -Filter 'jdk-21*' | Select-Object -First 1
if (-not $jdk) { throw '.tools에 JDK 21이 없습니다.' }
$env:JAVA_HOME = $jdk.FullName

Push-Location (Join-Path $projectRoot 'backend')
$exitCode = 0
try {
    & .\gradlew.bat bootRun --no-daemon
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $exitCode
