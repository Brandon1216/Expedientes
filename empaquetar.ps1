$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$dist = Join-Path $raiz 'dist'
$temporal = Join-Path $dist 'temporal'

function Paso($texto) { Write-Host "`n== $texto" -ForegroundColor Cyan }
function Revisar($que) { if ($LASTEXITCODE -ne 0) { throw "Falló: $que" } }

Paso 'Compilando la página (frontend)'
Push-Location (Join-Path $raiz 'frontend')
npm install --no-audit --no-fund; Revisar 'npm install'
npm run build; Revisar 'npm run build'
Pop-Location

Paso 'Compilando el servidor (backend)'
Push-Location (Join-Path $raiz 'backend')
mvn -q -DskipTests clean package; Revisar 'mvn package'
Pop-Location

Paso 'Preparando archivos'
if (Test-Path $dist) { Remove-Item $dist -Recurse -Force }
$entrada = New-Item -ItemType Directory -Force (Join-Path $temporal 'entrada')
$jar = Get-ChildItem (Join-Path $raiz 'backend\target') -Filter 'expedientes-backend-*.jar' |
    Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
Copy-Item $jar.FullName (Join-Path $entrada 'expedientes.jar')

Add-Type -AssemblyName System.Drawing
$logo = [System.Drawing.Image]::FromFile((Join-Path $raiz 'frontend\public\logo.png'))
$lienzo = New-Object System.Drawing.Bitmap 256, 256
$g = [System.Drawing.Graphics]::FromImage($lienzo)
$g.InterpolationMode = 'HighQualityBicubic'
$escala = [Math]::Min(256 / $logo.Width, 256 / $logo.Height)
$ancho = [int]($logo.Width * $escala); $alto = [int]($logo.Height * $escala)
$g.DrawImage($logo, [int]((256 - $ancho) / 2), [int]((256 - $alto) / 2), $ancho, $alto)
$png = New-Object System.IO.MemoryStream
$lienzo.Save($png, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $lienzo.Dispose(); $logo.Dispose()
$bytesPng = $png.ToArray()
$ico = New-Object System.IO.MemoryStream
$w = New-Object System.IO.BinaryWriter $ico
$w.Write([uint16]0); $w.Write([uint16]1); $w.Write([uint16]1)
$w.Write([byte]0); $w.Write([byte]0); $w.Write([byte]0); $w.Write([byte]0)
$w.Write([uint16]1); $w.Write([uint16]32)
$w.Write([uint32]$bytesPng.Length); $w.Write([uint32]22)
$w.Write($bytesPng); $w.Flush()
$rutaIco = Join-Path $temporal 'expedientes.ico'
[System.IO.File]::WriteAllBytes($rutaIco, $ico.ToArray())

Paso 'Creando Expedientes.exe con Java incluido'
jpackage --type app-image `
    --name Expedientes `
    --app-version 1.0.0 `
    --vendor 'S.U.T.S.P.E.M.I.D.B.C.' `
    --description 'Sistema de expedientes' `
    --input $entrada `
    --main-jar expedientes.jar `
    --arguments '--escritorio' `
    --java-options '-Dfile.encoding=UTF-8' `
    --icon $rutaIco `
    --dest $dist
Revisar 'jpackage'

Copy-Item (Join-Path $raiz 'instalador\*') $dist
Copy-Item (Join-Path $raiz 'frontend\public\logo.png') $dist
Remove-Item $temporal -Recurse -Force

Paso 'Comprimiendo para llevarlo a otra computadora'
$zip = Join-Path $dist 'Expedientes-instalador.zip'
Compress-Archive -Path (Join-Path $dist 'Expedientes'), (Join-Path $dist 'Instalar.bat'), (Join-Path $dist 'Instalar.ps1'), (Join-Path $dist 'logo.png') `
    -DestinationPath $zip

Write-Host "`nListo. Para instalar en esta computadora: doble clic en dist\Instalar.bat" -ForegroundColor Green
Write-Host "Para otra computadora: copia dist\Expedientes-instalador.zip, descomprímelo y abre Instalar.bat" -ForegroundColor Green
