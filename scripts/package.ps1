# Genera la distribucion y su checksum, sin instalar nada en el perfil del usuario.
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & .\mvnw.cmd -B -ntp clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron la compilacion o las pruebas.' }
    $version = (Get-Content -LiteralPath VERSION -Raw).Trim()
    $archive = Join-Path (Get-Location) "target\kumo-$version.zip"
    $hash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
    $name = [IO.Path]::GetFileName($archive)
    [IO.File]::WriteAllText("$archive.sha256", "$hash  $name`n", [Text.Encoding]::ASCII)
    Write-Host "Distribucion: $archive"
    Write-Host "SHA256: $archive.sha256"
} finally { Pop-Location }
