$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $environmentFile) {
    Write-Output 'Arquivo .env existente; credenciais mantidas.'
    return
}
function New-DemoSecret {
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $entropy = New-Object byte[] 24
        $generator.GetBytes($entropy)
        return -join ($entropy | ForEach-Object { $_.ToString('x2') })
    } finally { $generator.Dispose() }
}
$lines = @(
    "DB_PASSWORD=$(New-DemoSecret)"
    "RECEPTION_PASSWORD=$(New-DemoSecret)"
    "MANAGER_PASSWORD=$(New-DemoSecret)"
    "DOCTOR_PASSWORD=$(New-DemoSecret)"
    'CLINIC_ZONE=America/Cuiaba'
    'SPRING_PROFILES_ACTIVE=demo'
    'APP_DOMAIN='
)
[IO.File]::WriteAllText($environmentFile, (($lines -join "`n") + "`n"), [Text.UTF8Encoding]::new($false))
Write-Output 'Credenciais geradas em .env, ignorado pelo Git. Consulte esse arquivo localmente para entrar.'
