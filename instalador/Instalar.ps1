# Ventana de instalación de Expedientes: copia el programa a C:\Expedientes\programa y crea los accesos directos.
# Los datos (C:\Expedientes\datos) y los respaldos (C:\Expedientes\respaldos) no se tocan:
# sirve también para actualizar a una versión nueva.
#
# -Destino sirve para probar el instalador en otra carpeta sin tocar la instalación real.

param([string]$Destino = 'C:\Expedientes\programa')

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms, System.Drawing

trap {
    [Windows.Forms.MessageBox]::Show("No se pudo abrir el instalador.`n`n$_", 'Instalar Expedientes', 'OK', 'Error') | Out-Null
    exit 1
}

$origen = Join-Path $PSScriptRoot 'Expedientes'
$Destino = $Destino.TrimEnd('\')
$exeOrigen = Join-Path $origen 'Expedientes.exe'
$exe = Join-Path $Destino 'Expedientes.exe'

if (-not (Test-Path $exeOrigen)) {
    throw 'No encuentro la carpeta Expedientes junto a este archivo. Descomprime todo el .zip antes de instalar.'
}

# ---------- Lo que hace la instalación ----------

# Solo la copia que se va a reemplazar y la del paquete: si la del paquete sigue abierta
# ocupa el puerto y el programa nuevo solo abriría el navegador hacia la versión vieja.
function Programas-Abiertos {
    @(Get-Process Expedientes -ErrorAction SilentlyContinue | Where-Object {
        $_.Path -and ($_.Path -ieq $exe -or $_.Path -ieq $exeOrigen)
    })
}

function Crear-Acceso($carpeta, $programa) {
    $shell = New-Object -ComObject WScript.Shell
    $acceso = $shell.CreateShortcut((Join-Path $carpeta 'Expedientes.lnk'))
    $acceso.TargetPath = $programa
    $acceso.WorkingDirectory = Split-Path $programa
    $acceso.IconLocation = "$programa,0"
    $acceso.Description = 'Sistema de expedientes'
    $acceso.Save()
}

function Instalar {
    $script:trabajando = $true
    Mostrar $pasoTrabajo
    try {
        $abiertos = Programas-Abiertos
        if ($abiertos.Count -gt 0) {
            Avance 'Cerrando Expedientes...'
            $abiertos | Stop-Process -Force
            $limite = (Get-Date).AddSeconds(10)
            while (($abiertos | Where-Object { -not $_.HasExited }) -and (Get-Date) -lt $limite) {
                Avance 'Cerrando Expedientes...'
                Start-Sleep -Milliseconds 40
            }
        }

        Avance 'Copiando el programa...'
        New-Item -ItemType Directory -Force $Destino | Out-Null
        # Sin /R y /W, robocopy reintenta por días si un archivo está en uso
        $copia = Start-Process robocopy -WindowStyle Hidden -PassThru -ArgumentList `
            "`"$origen`" `"$Destino`" /MIR /R:3 /W:2 /NFL /NDL /NJH /NJS /NP"
        $null = $copia.Handle # sin esto, ExitCode queda vacío al terminar
        while (-not $copia.HasExited) {
            Avance 'Copiando el programa...'
            Start-Sleep -Milliseconds 40
        }
        if ($copia.ExitCode -ge 8) {
            throw "No se pudieron copiar los archivos del programa (código $($copia.ExitCode)). Cierra Expedientes y vuelve a intentarlo."
        }

        Avance 'Creando accesos directos...'
        if ($marcaEscritorio.Checked) { Crear-Acceso ([Environment]::GetFolderPath('Desktop')) $exe }
        if ($marcaInicio.Checked) { Crear-Acceso (Join-Path ([Environment]::GetFolderPath('StartMenu')) 'Programs') $exe }

        $finComo.Text = if ($marcaEscritorio.Checked) { 'Ábrelo con el ícono "Expedientes" del Escritorio.' }
                        elseif ($marcaInicio.Checked) { 'Ábrelo desde "Expedientes" en el menú Inicio.' }
                        else { "Ábrelo desde $exe" }
        $script:trabajando = $false
        Mostrar $pasoFin
    } catch {
        $script:trabajando = $false
        $errorTexto.Text = "$_"
        Mostrar $pasoError
    }
}

# ---------- Ventana ----------

# Sin esto Windows la dibuja borrosa en pantallas con escala mayor a 100 %
Add-Type -Namespace Instalador -Name Pantalla -MemberDefinition @'
[DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
[DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr ventana, int modo);
[DllImport("kernel32.dll")] public static extern IntPtr GetConsoleWindow();
'@
[Instalador.Pantalla]::SetProcessDPIAware() | Out-Null
[Windows.Forms.Application]::EnableVisualStyles()

# Instalar.bat abre PowerShell oculto y Windows aplica ese "oculto" al primer ShowWindow del proceso:
# se gasta aquí, en la consola, para que no esconda la ventana del instalador.
[Instalador.Pantalla]::ShowWindow([Instalador.Pantalla]::GetConsoleWindow(), 0) | Out-Null

$grafico = [Drawing.Graphics]::FromHwnd([IntPtr]::Zero)
$escala = $grafico.DpiX / 96
$grafico.Dispose()
function U($n) { [int]($n * $escala) }

# Los mismos colores de styles.css
function Color($html) { [Drawing.ColorTranslator]::FromHtml($html) }
$guinda = Color '#7d1a27'
$guindaOscuro = Color '#3d0c14'
$dorado = Color '#b8902b'
$doradoClaro = Color '#e6cd86'
$tinta = Color '#221b1d'
$gris = Color '#6f6569'
$lineaFuerte = Color '#cfc8c2'
$tenue = Color '#eeebe8'
$peligro = Color '#b3392f'
$activo = Color '#22693f'

function Fuente($puntos, $estilo = 'Regular', $familia = 'Segoe UI') { New-Object Drawing.Font $familia, $puntos, ([Drawing.FontStyle]$estilo) }

function Texto($padre, $texto, $x, $y, $ancho, $alto, $fuente, $color, $alinear = 'TopLeft') {
    $e = New-Object Windows.Forms.Label
    $e.Text = $texto
    $e.Location = New-Object Drawing.Point (U $x), (U $y)
    $e.Size = New-Object Drawing.Size (U $ancho), (U $alto)
    $e.Font = $fuente
    $e.ForeColor = $color
    $e.BackColor = [Drawing.Color]::Transparent
    $e.TextAlign = $alinear
    $e.UseMnemonic = $false
    $padre.Controls.Add($e)
    $e
}

function Boton($padre, $texto, $derecha, $ancho, $primario) {
    $b = New-Object Windows.Forms.Button
    $b.Text = $texto
    $b.Size = New-Object Drawing.Size (U $ancho), (U 38)
    $b.Location = New-Object Drawing.Point (U ($derecha - $ancho)), (U 372)
    $b.Font = Fuente 10
    $b.FlatStyle = 'Flat'
    $b.Cursor = 'Hand'
    $b.UseVisualStyleBackColor = $false
    if ($primario) {
        $b.BackColor = $guinda
        $b.ForeColor = [Drawing.Color]::White
        $b.FlatAppearance.BorderSize = 0
    } else {
        $b.BackColor = [Drawing.Color]::White
        $b.ForeColor = $tinta
        $b.FlatAppearance.BorderColor = $lineaFuerte
    }
    $padre.Controls.Add($b)
    $b
}

function Marca($padre, $texto, $y) {
    $m = New-Object Windows.Forms.CheckBox
    $m.Text = $texto
    $m.Checked = $true
    $m.Location = New-Object Drawing.Point (U 36), (U $y)
    $m.Size = New-Object Drawing.Size (U 370), (U 26)
    $m.Font = Fuente 10
    $m.ForeColor = $tinta
    $m.Cursor = 'Hand'
    $padre.Controls.Add($m)
    $m
}

function Dato($padre, $etiqueta, $valor, $y) {
    Texto $padre $etiqueta 36 $y 370 20 (Fuente 9) $gris | Out-Null
    Texto $padre $valor 36 ($y + 19) 370 24 (Fuente 10) $tinta | Out-Null
}

function Paso {
    $p = New-Object Windows.Forms.Panel
    $p.Location = New-Object Drawing.Point (U 243), 0
    $p.Size = New-Object Drawing.Size (U 442), (U 440)
    $p.BackColor = [Drawing.Color]::White
    $p.Visible = $false
    $ventana.Controls.Add($p)
    $p
}

function Mostrar($paso) {
    foreach ($p in $pasoInicio, $pasoTrabajo, $pasoFin, $pasoError) { $p.Visible = $p -eq $paso }
    $ventana.Refresh()
}

# La barra va y viene mientras se trabaja: robocopy no informa cuánto falta
$script:posicion = 0
function Avance($estado) {
    if ($trabajoEstado.Text -ne $estado) { $trabajoEstado.Text = $estado }
    $script:posicion = ($script:posicion + (U 5)) % ($barra.Width + $barraTramo.Width)
    $barraTramo.Left = $script:posicion - $barraTramo.Width
    [Windows.Forms.Application]::DoEvents()
}

$ventana = New-Object Windows.Forms.Form
$ventana.Text = 'Instalar Expedientes'
$ventana.ClientSize = New-Object Drawing.Size (U 685), (U 440)
$ventana.FormBorderStyle = 'FixedSingle'
$ventana.MaximizeBox = $false
$ventana.StartPosition = 'CenterScreen'
$ventana.BackColor = [Drawing.Color]::White
$ventana.Icon = [Drawing.Icon]::ExtractAssociatedIcon($exeOrigen)

$script:trabajando = $false
$ventana.add_FormClosing({ if ($script:trabajando) { $_.Cancel = $true } })
$ventana.add_Shown({ $ventana.Activate() })

# Panel de la marca, como en la pantalla de inicio de sesión
$marca = New-Object Windows.Forms.Panel
$marca.Location = New-Object Drawing.Point 0, 0
$marca.Size = New-Object Drawing.Size (U 240), (U 440)
$marca.BackColor = $guindaOscuro
$ventana.Controls.Add($marca)

$filo = New-Object Windows.Forms.Panel
$filo.Location = New-Object Drawing.Point (U 240), 0
$filo.Size = New-Object Drawing.Size (U 3), (U 440)
$filo.BackColor = $dorado
$ventana.Controls.Add($filo)

$rutaLogo = Join-Path $PSScriptRoot 'logo.png'
if (Test-Path $rutaLogo) {
    $logo = New-Object Windows.Forms.PictureBox
    $logo.Image = [Drawing.Image]::FromFile($rutaLogo)
    $logo.SizeMode = 'Zoom'
    $logo.BackColor = [Drawing.Color]::Transparent
    $logo.Location = New-Object Drawing.Point (U 40), (U 92)
    $logo.Size = New-Object Drawing.Size (U 160), (U 146)
    $marca.Controls.Add($logo)
}
Texto $marca 'Sistema de Expedientes' 10 252 220 28 (Fuente 12 'Bold') ([Drawing.Color]::White) 'MiddleCenter' | Out-Null
Texto $marca 'S.U.T.S.P.E.M.I.D.B.C.' 10 280 220 22 (Fuente 9 'Bold') $doradoClaro 'MiddleCenter' | Out-Null
$version = (Get-Item $exeOrigen).VersionInfo.ProductVersion
if ($version) {
    Texto $marca "Versión $version" 10 400 220 20 (Fuente 8.5) (Color '#c9b8ba') 'MiddleCenter' | Out-Null
}

# Paso 1: confirmar
$actualizando = Test-Path $exe
$pasoInicio = Paso
Texto $pasoInicio $(if ($actualizando) { 'Actualizar Expedientes' } else { 'Instalar Expedientes' }) 36 30 370 40 (Fuente 18 'Bold') $tinta | Out-Null
Texto $pasoInicio $(if ($actualizando) {
        'Se reemplazará el programa por esta versión. Tus expedientes y respaldos no se modifican.'
    } else {
        'Se instalará el programa en esta computadora. Ya trae todo lo necesario: no hay que instalar nada más.'
    }) 36 74 370 46 (Fuente 10) $gris | Out-Null
Dato $pasoInicio 'Carpeta del programa' $Destino 134
$marcaEscritorio = Marca $pasoInicio 'Crear acceso directo en el Escritorio' 200
$marcaInicio = Marca $pasoInicio 'Agregar al menú Inicio' 228
if ((Programas-Abiertos).Count -gt 0) {
    Texto $pasoInicio 'Expedientes está abierto: se cerrará para continuar.' 36 268 370 44 (Fuente 9.5) $peligro | Out-Null
}
$botonInstalar = Boton $pasoInicio $(if ($actualizando) { 'Actualizar' } else { 'Instalar' }) 406 120 $true
$botonCancelar = Boton $pasoInicio 'Cancelar' 276 100 $false
$botonCancelar.add_Click({ $ventana.Close() })
# Con un temporizador el clic termina primero y la ventana alcanza a mostrar el paso siguiente
$arranque = New-Object Windows.Forms.Timer
$arranque.Interval = 50
$arranque.add_Tick({ $arranque.Stop(); Instalar })
$botonInstalar.add_Click({ $arranque.Start() })
$ventana.AcceptButton = $botonInstalar

# Paso 2: trabajando
$pasoTrabajo = Paso
Texto $pasoTrabajo $(if ($actualizando) { 'Actualizando...' } else { 'Instalando...' }) 36 150 370 40 (Fuente 18 'Bold') $tinta | Out-Null
$trabajoEstado = Texto $pasoTrabajo 'Preparando...' 36 194 370 24 (Fuente 10) $gris
$barra = New-Object Windows.Forms.Panel
$barra.Location = New-Object Drawing.Point (U 36), (U 230)
$barra.Size = New-Object Drawing.Size (U 370), (U 6)
$barra.BackColor = $tenue
$pasoTrabajo.Controls.Add($barra)
$barraTramo = New-Object Windows.Forms.Panel
$barraTramo.Size = New-Object Drawing.Size (U 110), (U 6)
$barraTramo.BackColor = $guinda
$barra.Controls.Add($barraTramo)

# Paso 3: terminado
$pasoFin = Paso
Texto $pasoFin $(if ($actualizando) { 'Expedientes quedó actualizado' } else { 'Expedientes quedó instalado' }) 36 30 370 40 (Fuente 18 'Bold') $activo | Out-Null
$finComo = Texto $pasoFin '' 36 74 370 46 (Fuente 10) $gris
Dato $pasoFin 'Tus datos' 'C:\Expedientes\datos' 134
Dato $pasoFin 'Respaldos (uno por día)' 'C:\Expedientes\respaldos' 190
$marcaAbrir = Marca $pasoFin 'Abrir Expedientes ahora' 262
$botonFin = Boton $pasoFin 'Finalizar' 406 120 $true
$botonFin.add_Click({
    if ($marcaAbrir.Checked) { Start-Process $exe -WorkingDirectory $Destino }
    $ventana.Close()
})

# Si algo falla
$pasoError = Paso
Texto $pasoError 'No se pudo completar' 36 30 370 40 (Fuente 18 'Bold') $peligro | Out-Null
$errorTexto = Texto $pasoError '' 36 74 370 200 (Fuente 10) $tinta
$botonReintentar = Boton $pasoError 'Reintentar' 406 120 $true
$botonReintentar.add_Click({ Mostrar $pasoInicio })
$botonCerrar = Boton $pasoError 'Cerrar' 276 100 $false
$botonCerrar.add_Click({ $ventana.Close() })

Mostrar $pasoInicio
[Windows.Forms.Application]::Run($ventana)
