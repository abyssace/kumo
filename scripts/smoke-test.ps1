# Pruebas de instalacion reales sobre el ZIP. Todo queda en una carpeta temporal exclusiva.
param([string]$Archive, [string]$TempRoot = [IO.Path]::GetTempPath())
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = Split-Path -Parent $PSScriptRoot
$version = (Get-Content -LiteralPath (Join-Path $root 'VERSION') -Raw).Trim()
if (!$Archive) { $Archive = Join-Path $root "target\kumo-$version.zip" }
$temp = Join-Path $TempRoot ("kumo smoke " + [Guid]::NewGuid().ToString('N'))
$oldHome = $env:KUMO_HOME
$oldJava = $env:JAVA_HOME
$oldUserPath = [Environment]::GetEnvironmentVariable('Path', 'User')

function Assert-True([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "Fallo de prueba: $Message" }
}

function Invoke-TestCommand {
    param([string]$Program, [string[]]$Arguments, [int]$Expected = 0)
    $preference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = @(& $Program @Arguments 2>&1)
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $preference }
    $text = ($output | ForEach-Object { $_.ToString() }) -join "`n"
    if ($code -ne $Expected) { throw "Salida $code, se esperaba $Expected. $text" }
    return $text
}

try {
    New-Item -ItemType Directory -Path $temp | Out-Null
    Expand-Archive -LiteralPath $Archive -DestinationPath (Join-Path $temp 'unpacked')
    $package = Join-Path $temp "unpacked\kumo-$version"
    foreach ($file in @('kumo.jar', 'lib', 'bin\kumo.cmd', 'bin\kumo.ps1', 'bin\kumo',
            'install.ps1', 'install.sh', 'README.md', 'LICENSE', 'VERSION', 'examples\brief-template.json')) {
        Assert-True (Test-Path -LiteralPath (Join-Path $package $file)) "Falta $file en el ZIP."
    }
    Assert-True (!(Test-Path -LiteralPath (Join-Path $package '.kumo'))) 'El ZIP no debe incluir indices.'
    $env:KUMO_HOME = Join-Path $temp 'config existente'
    New-Item -ItemType Directory -Path $env:KUMO_HOME | Out-Null
    [IO.File]::WriteAllText((Join-Path $env:KUMO_HOME 'config.json'), '{}')
    [IO.File]::WriteAllText((Join-Path $env:KUMO_HOME 'brief-template.json'), 'NO MODIFICAR')
    $work = Join-Path $temp 'corpus con espacios'
    New-Item -ItemType Directory -Path (Join-Path $work '.kumo') | Out-Null
    [IO.File]::WriteAllText((Join-Path $work '.kumo\index.json'), 'INDICE DEL USUARIO')
    $portable = Join-Path $package 'bin\kumo.cmd'
    Assert-True ((Invoke-TestCommand $portable @('--version')).Trim() -eq "kumo $version") 'Version portable.'
    $destination = Join-Path $temp 'app con espacios'
    $installer = Join-Path $package 'install.ps1'
    $installArguments = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $installer, '-InstallDir', $destination)
    Invoke-TestCommand 'powershell.exe' $installArguments | Out-Null
    $launcher = Join-Path $destination 'bin\kumo.cmd'
    Assert-True ((Invoke-TestCommand $launcher @('--version')).Trim() -eq "kumo $version") 'Version instalada.'
    Assert-True ((Invoke-TestCommand $launcher @('--help')).Contains('Comandos:')) 'Ayuda en espanol.'
    Invoke-TestCommand $launcher @('--opcion-inexistente') 2 | Out-Null
    Push-Location $work
    try {
        $errorText = Invoke-TestCommand $launcher @('brief', 'archivo inexistente.txt') 1
        Assert-True ($errorText.Contains($work)) 'El lanzador debe conservar el directorio del corpus y los espacios.'
    } finally { Pop-Location }
    $pointer = Get-Content -LiteralPath (Join-Path $destination 'current') -Raw
    Invoke-TestCommand 'powershell.exe' $installArguments | Out-Null
    Assert-True ((Get-Content -LiteralPath (Join-Path $destination 'current') -Raw) -ne $pointer) 'Actualiza la referencia.'
    Assert-True (@(Get-ChildItem -LiteralPath (Join-Path $destination 'releases') -Directory).Count -eq 2) 'Conserva la version anterior.'
    Assert-True ((Get-Content -LiteralPath (Join-Path $env:KUMO_HOME 'config.json') -Raw) -eq '{}') 'Conserva config.json.'
    Assert-True ((Get-Content -LiteralPath (Join-Path $env:KUMO_HOME 'brief-template.json') -Raw) -eq 'NO MODIFICAR') 'Conserva la plantilla.'
    Assert-True ((Get-Content -LiteralPath (Join-Path $work '.kumo\index.json') -Raw) -eq 'INDICE DEL USUARIO') 'Conserva el indice.'
    Assert-True ([Environment]::GetEnvironmentVariable('Path', 'User') -eq $oldUserPath) 'No modifica PATH sin -AddToPath.'
    Assert-True ((Invoke-TestCommand (Join-Path $root 'bin\kumo.cmd') @('--version')).Trim() -eq "kumo $version") 'Lanzador del repositorio.'
    Invoke-TestCommand 'powershell.exe' @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
        (Join-Path $root 'install.ps1'), '-SkipBuild', '-InstallDir', (Join-Path $temp 'desde repo')) | Out-Null
    $activeBeforeFailure = Get-Content -LiteralPath (Join-Path $destination 'current') -Raw
    [IO.File]::WriteAllText((Join-Path $package 'VERSION'), '9.9.9')
    Invoke-TestCommand 'powershell.exe' $installArguments 1 | Out-Null
    Assert-True ((Get-Content -LiteralPath (Join-Path $destination 'current') -Raw) -eq $activeBeforeFailure) 'No activa un paquete incompatible.'
    Assert-True (@(Get-ChildItem -LiteralPath (Join-Path $destination 'releases') -Filter '.stage-*' -Directory).Count -eq 0) 'Limpia solo su staging fallido.'
    [IO.File]::WriteAllText((Join-Path $package 'VERSION'), $version)
    $foreign = Join-Path $temp 'carpeta ajena'
    New-Item -ItemType Directory -Path $foreign | Out-Null
    [IO.File]::WriteAllText((Join-Path $foreign 'usuario.txt'), 'CONSERVAR')
    Invoke-TestCommand 'powershell.exe' @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $installer,
        '-InstallDir', $foreign) 1 | Out-Null
    Assert-True ((Get-Content -LiteralPath (Join-Path $foreign 'usuario.txt') -Raw) -eq 'CONSERVAR') 'No pisa carpetas ajenas.'
    $active = Get-Content -LiteralPath (Join-Path $destination 'current') -Raw
    $env:JAVA_HOME = Join-Path $temp 'java inexistente'
    Invoke-TestCommand $launcher @('--version') 1 | Out-Null
    Invoke-TestCommand 'powershell.exe' $installArguments 1 | Out-Null
    Assert-True ((Get-Content -LiteralPath (Join-Path $destination 'current') -Raw) -eq $active) 'Fallo de Java no cambia la version activa.'
    Write-Host 'OK: ZIP, repositorio, instalacion, actualizacion, fallos seguros, argumentos y conservacion de datos (Windows).'
} finally {
    $env:KUMO_HOME = $oldHome
    $env:JAVA_HOME = $oldJava
    # Se elimina solo el directorio exclusivo creado por esta prueba, nunca una instalacion real.
    if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
}
