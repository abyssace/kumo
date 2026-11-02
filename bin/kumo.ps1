# Lanzador Windows. Compatible con Windows PowerShell 5.1; mensajes ASCII a proposito.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
Set-StrictMode -Version Latest
# PowerShell 5.1 pierde comillas/argumentos vacios al invocar nativos con &.
# Se construye la linea Win32 con escape de comillas y barras finales, sin usar un shell.
function ConvertTo-NativeArgument([string]$Value) {
    $escaped = [regex]::Replace($Value, '(\\*)"', '$1$1\"')
    $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
    return '"' + $escaped + '"'
}
try {
    $root = Split-Path -Parent $PSScriptRoot
    if (Test-Path -LiteralPath (Join-Path $root '.kumo-install')) {
        $slot = (Get-Content -LiteralPath (Join-Path $root 'current') -Raw).Trim()
        if ($slot -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$') { throw 'Referencia de version invalida.' }
        $payload = Join-Path $root "releases\$slot"
    } elseif (Test-Path -LiteralPath (Join-Path $root 'kumo.jar')) {
        $payload = $root
    } else {
        $payload = Join-Path $root 'target'
    }
    $jar = Join-Path $payload 'kumo.jar'
    if (!(Test-Path -LiteralPath $jar -PathType Leaf) -or
        !(Test-Path -LiteralPath (Join-Path $payload 'lib') -PathType Container)) {
        throw 'Falta kumo.jar o lib/. Compila o reinstala kumo.'
    }
    if ($env:JAVA_HOME) { $java = Join-Path $env:JAVA_HOME 'bin\java.exe' }
    else { $java = (Get-Command java -CommandType Application -ErrorAction Stop).Source }
    $version = @(& $java --version 2>$null)
    if ($LASTEXITCODE -ne 0 -or $version.Count -eq 0 -or
        $version[0] -notmatch '(?:openjdk|java)\s+(?:version\s+)?"?(\d+)') {
        throw 'No se pudo comprobar Java; instala Java 21 o superior y revisa JAVA_HOME.'
    }
    if ([int]$Matches[1] -lt 21) { throw 'Se necesita Java 21 o superior.' }
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = $java
    $start.UseShellExecute = $false
    $start.WorkingDirectory = (Get-Location).ProviderPath
    $nativeArguments = @('-jar', $jar) + @($args)
    $start.Arguments = ($nativeArguments | ForEach-Object { ConvertTo-NativeArgument $_ }) -join ' '
    $process = [Diagnostics.Process]::Start($start)
    $process.WaitForExit()
    exit $process.ExitCode
} catch {
    [Console]::Error.WriteLine("Error: " + $_.Exception.Message)
    exit 1
}
