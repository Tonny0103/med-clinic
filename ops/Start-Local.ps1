param([switch]$Empty)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'New-DemoEnvironment.ps1')
foreach ($line in Get-Content -LiteralPath (Join-Path $projectRoot '.env')) {
    $pair = $line.Split('=', 2)
    if ($pair.Count -eq 2 -and $pair[0] -in @('RECEPTION_PASSWORD', 'MANAGER_PASSWORD', 'DOCTOR_PASSWORD', 'CLINIC_ZONE')) {
        [Environment]::SetEnvironmentVariable($pair[0], $pair[1], 'Process')
    }
}
if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    $jdkRoot = Join-Path $env:USERPROFILE '.jdks'
    $java21 = Get-ChildItem -LiteralPath $jdkRoot -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '21' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe')) } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($java21) { $env:JAVA_HOME = $java21.FullName }
    else { throw 'Instale JDK 21 e configure JAVA_HOME.' }
}
$env:COOKIE_SECURE = 'false'
Push-Location $projectRoot
try {
    if ($Empty) { & .\mvnw.cmd -B -ntp spring-boot:run }
    else { & .\mvnw.cmd -B -ntp spring-boot:run '-Dspring-boot.run.profiles=demo' }
} finally { Pop-Location }
