# Instalacion por usuario, sin descargas de modelos ni cambios de configuracion.
[CmdletBinding()]
param(
    [string]$InstallDir = (Join-Path $env:LOCALAPPDATA 'Programs\kumo'),
    [switch]$AddToPath,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$stage = $null
try {
    if ($env:JAVA_HOME) { $java = Join-Path $env:JAVA_HOME 'bin\java.exe' }
    else { $java = (Get-Command java -CommandType Application -ErrorAction Stop).Source }
    $javaVersion = @(& $java --version 2>$null)
    if ($LASTEXITCODE -ne 0 -or $javaVersion.Count -eq 0 -or
        $javaVersion[0] -notmatch '(?:openjdk|java)\s+(?:version\s+)?"?(\d+)' -or [int]$Matches[1] -lt 21) {
        throw 'Se necesita Java 21 o superior. Revisa JAVA_HOME; no se instalara automaticamente.'
    }
    $source = $PSScriptRoot
    $version = (Get-Content -LiteralPath (Join-Path $source 'VERSION') -Raw).Trim()
    if ($version -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$') { throw 'VERSION invalida.' }
    if (Test-Path -LiteralPath (Join-Path $source 'kumo.jar') -PathType Leaf) {
        $payload = $source
    } else {
        if (!$SkipBuild) {
            if (!(Test-Path -LiteralPath (Join-Path (Split-Path $java) 'javac.exe'))) {
                throw 'Para compilar necesitas un JDK 21 o superior, no solo un runtime.'
            }
            Push-Location $source
            try {
                & (Join-Path $source 'mvnw.cmd') -B -ntp clean verify
                if ($LASTEXITCODE -ne 0) { throw 'La compilacion o las pruebas fallaron. No se activo ninguna version.' }
            } finally { Pop-Location }
        }
        $payload = Join-Path $source 'target'
    }
    if (!(Test-Path -LiteralPath (Join-Path $payload 'kumo.jar') -PathType Leaf) -or
        !(Test-Path -LiteralPath (Join-Path $payload 'lib') -PathType Container)) {
        throw 'Falta kumo.jar o lib/. Compila primero o extrae el ZIP completo.'
    }
    $libraries = @(Get-ChildItem -LiteralPath (Join-Path $payload 'lib') -Filter '*.jar' -File)
    if ($libraries.Count -eq 0) { throw 'No hay dependencias en lib/.' }
    $root = [IO.Path]::GetFullPath($InstallDir)
    if ($AddToPath -and $root.Contains(';')) { throw 'La ruta no puede contener punto y coma al agregarla a PATH.' }
    $marker = Join-Path $root '.kumo-install'
    if (Test-Path -LiteralPath $root) {
        $item = Get-Item -LiteralPath $root -Force
        if (!$item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'El destino debe ser una carpeta normal, no un archivo ni un enlace.'
        }
        if (Test-Path -LiteralPath $marker) {
            if ((Get-Content -LiteralPath $marker -Raw).Trim() -ne 'kumo installer v1') {
                throw 'El marcador de instalacion no pertenece a kumo.'
            }
        } elseif (@(Get-ChildItem -LiteralPath $root -Force).Count -gt 0) {
            throw 'El destino contiene archivos ajenos a kumo. Elige una carpeta nueva o vacia.'
        }
    }
    foreach ($name in @('bin', 'releases')) {
        $path = Join-Path $root $name
        if ((Test-Path -LiteralPath $path) -and
            ((Get-Item -LiteralPath $path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'No se instalaran archivos a traves de enlaces.'
        }
    }
    New-Item -ItemType Directory -Path (Join-Path $root 'releases') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $root 'bin') -Force | Out-Null
    $slot = "$version-$([Guid]::NewGuid().ToString('N'))"
    $stage = Join-Path $root "releases\.stage-$slot"
    New-Item -ItemType Directory -Path (Join-Path $stage 'lib') | Out-Null
    Copy-Item -LiteralPath (Join-Path $payload 'kumo.jar') -Destination $stage
    foreach ($document in @('LICENSE', 'README.md', 'CHANGELOG.md', 'VERSION')) {
        Copy-Item -LiteralPath (Join-Path $source $document) -Destination $stage
    }
    foreach ($library in $libraries) { Copy-Item -LiteralPath $library.FullName -Destination (Join-Path $stage 'lib') }
    $check = @(& $java -jar (Join-Path $stage 'kumo.jar') --version)
    if ($LASTEXITCODE -ne 0 -or ($check -join '').Trim() -ne "kumo $version") {
        throw 'El paquete no arranca o VERSION no coincide con el manifiesto.'
    }
    Move-Item -LiteralPath $stage -Destination (Join-Path $root "releases\$slot")
    $stage = $null
    foreach ($launcher in @('kumo.cmd', 'kumo.ps1')) {
        Copy-Item -LiteralPath (Join-Path $source "bin\$launcher") -Destination (Join-Path $root 'bin') -Force
    }
    # La referencia se actualiza solo cuando el jar y los lanzadores estan listos.
    [IO.File]::WriteAllText($marker, "kumo installer v1`n")
    $pointer = Join-Path $root "current-$slot.tmp"
    [IO.File]::WriteAllText($pointer, "$slot`n")
    Move-Item -LiteralPath $pointer -Destination (Join-Path $root 'current') -Force
    if ($AddToPath) {
        $bin = Join-Path $root 'bin'
        $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
        $entries = @($userPath -split ';' | Where-Object { $_ })
        if (!($entries | Where-Object { $_.TrimEnd('\') -ieq $bin.TrimEnd('\') })) {
            $updated = (@($entries) + @($bin)) -join ';'
            [Environment]::SetEnvironmentVariable('Path', $updated, 'User')
        }
        Write-Host 'PATH de usuario actualizado. Abre una terminal nueva.'
    } else {
        Write-Host 'PATH no modificado. Puedes repetir con -AddToPath o usar el lanzador por ruta.'
    }
    Write-Host "Instalado kumo $version en $root"
    Write-Host "Lanzador: $(Join-Path $root 'bin\kumo.cmd')"
    Write-Host 'Configuracion, plantilla e indices conservados. Ejecuta kumo init cuando quieras.'
} catch {
    [Console]::Error.WriteLine('Error: ' + $_.Exception.Message)
    exit 1
} finally {
    # Solo se limpia la carpeta temporal exclusiva que acaba de crear este proceso.
    if ($stage -and (Test-Path -LiteralPath $stage)) { Remove-Item -LiteralPath $stage -Recurse -Force }
}
