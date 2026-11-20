param(
    [switch]$StartOracleServices,
    [string]$OracleServiceNames
)

$ErrorActionPreference = 'Stop'

if ($StartOracleServices) {
    foreach ($serviceName in ($OracleServiceNames -split ',')) {
        $service = Get-Service -Name $serviceName
        if ($service.Status -ne 'Running') {
            Start-Service -Name $serviceName
        }
        (Get-Service -Name $serviceName).WaitForStatus(
            [System.ServiceProcess.ServiceControllerStatus]::Running,
            [TimeSpan]::FromSeconds(120)
        )
    }
    exit 0
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'frontend'
$jarPath = Join-Path $backendDir 'target\cinerama-1.0.0-SNAPSHOT.jar'
$ngCli = Join-Path $frontendDir 'node_modules\@angular\cli\bin\ng.js'
$java = Get-Command java.exe -ErrorAction SilentlyContinue
$node = Get-Command node.exe -ErrorAction SilentlyContinue
$npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
$backendProcess = $null
$frontendProcess = $null
$logs = Join-Path $env:TEMP ('cinerama-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$backendOut = "$logs-backend.out.log"
$backendErr = "$logs-backend.err.log"
$frontendOut = "$logs-frontend.out.log"
$frontendErr = "$logs-frontend.err.log"

function Test-Port {
    param([int]$Port)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connect = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if (-not $connect.AsyncWaitHandle.WaitOne(1000)) {
            return $false
        }
        $client.EndConnect($connect)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Wait-ForHttp {
    param(
        [string]$Uri,
        [int]$TimeoutSeconds,
        [System.Diagnostics.Process]$Process,
        [string]$Component
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $Process.Refresh()
        if ($Process.HasExited) {
            Write-Host "$Component terminó inesperadamente (código $($Process.ExitCode))."
            return $false
        }
        try {
            $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                return $true
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Test-BackendBuildRequired {
    param([string]$BackendDir, [string]$JarPath)

    if (-not (Test-Path $JarPath)) {
        return $true
    }

    $referencia = (Get-Item $JarPath).LastWriteTime
    $pom = Get-Item (Join-Path $BackendDir 'pom.xml') -ErrorAction SilentlyContinue
    if ($null -ne $pom -and $pom.LastWriteTime -gt $referencia) {
        return $true
    }

    $fuentes = Get-ChildItem (Join-Path $BackendDir 'src') -Recurse -File -ErrorAction SilentlyContinue
    return @($fuentes | Where-Object { $_.LastWriteTime -gt $referencia }).Count -gt 0
}

function Stop-AppProcess {
    param([System.Diagnostics.Process]$Process)

    if ($null -ne $Process) {
        $Process.Refresh()
        if (-not $Process.HasExited) {
            Stop-Process -Id $Process.Id -ErrorAction SilentlyContinue
        }
    }
}

try {
    if (-not $java) {
        throw 'No se encontró Java. Instala Java 21 y vuelve a iniciar Cinerama.'
    }
    if (-not $node -or -not $npm) {
        throw 'No se encontró Node.js/npm. Instala Node.js 18 o posterior y vuelve a iniciar Cinerama.'
    }

    $databaseService = Get-Service -Name 'OracleServiceXE' -ErrorAction SilentlyContinue
    $listenerServices = @(Get-Service -Name 'Oracle*Listener' -ErrorAction SilentlyContinue)
    if (-not $databaseService -or $listenerServices.Count -eq 0) {
        throw 'No se encontraron los servicios de Oracle XE. Instala Oracle XE y configura la base de datos local.'
    }
    if ($listenerServices.Count -gt 1) {
        throw 'Se encontraron varios listeners de Oracle. Deja activo solo el listener correspondiente a Oracle XE.'
    }

    $servicesToStart = @($databaseService.Name, $listenerServices[0].Name)
    $servicesStopped = @($servicesToStart | Where-Object {
        (Get-Service -Name $_).Status -ne 'Running'
    })
    if ($servicesStopped.Count -gt 0) {
        Write-Host 'Iniciando Oracle XE. Windows puede pedir permiso de administrador...'
        $serviceNames = $servicesToStart -join ','
        $arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -StartOracleServices -OracleServiceNames `"$serviceNames`""
        $serviceProcess = Start-Process -FilePath (Join-Path $PSHOME 'powershell.exe') `
            -ArgumentList $arguments -Verb RunAs -Wait -PassThru
        if ($serviceProcess.ExitCode -ne 0) {
            throw 'No se pudieron iniciar los servicios de Oracle. Acepta el permiso de Windows e inténtalo otra vez.'
        }
    }

    Write-Host 'Esperando a que Oracle escuche en el puerto 1521...'
    $oracleDeadline = (Get-Date).AddSeconds(120)
    while (-not (Test-Port -Port 1521) -and (Get-Date) -lt $oracleDeadline) {
        Start-Sleep -Seconds 2
    }
    if (-not (Test-Port -Port 1521)) {
        throw 'Oracle XE no respondió en el puerto 1521. Revisa sus servicios y la configuración local de Oracle.'
    }

    if (Test-BackendBuildRequired -BackendDir $backendDir -JarPath $jarPath) {
        # Sin esto se ejecutaba un jar viejo: la web cargaba datos pero el login
        # respondía sin token porque el backend compilado no tenía los cambios.
        Write-Host 'Compilando el backend con el código actual...'
        Push-Location $backendDir
        try {
            & (Join-Path $backendDir 'mvnw.cmd') -q -DskipTests package
            if ($LASTEXITCODE -ne 0) {
                throw 'Falló la compilación del backend con Maven.'
            }
        } finally {
            Pop-Location
        }
    }
    if (-not (Test-Path $jarPath)) {
        throw 'No se encontró el backend compilado después de la compilación.'
    }

    if (-not (Test-Path $ngCli)) {
        Write-Host 'Instalando las dependencias del frontend...'
        Push-Location $frontendDir
        try {
            & $npm.Source install
            if ($LASTEXITCODE -ne 0) {
                throw 'Falló la instalación de dependencias del frontend.'
            }
        } finally {
            Pop-Location
        }
    }
    if (-not (Test-Path $ngCli)) {
        throw 'No se encontró Angular CLI después de instalar las dependencias.'
    }

    Write-Host 'Iniciando backend con Oracle...'
    $backendProcess = Start-Process -FilePath $java.Source `
        -ArgumentList ('-jar "' + $jarPath + '"') `
        -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput "$backendOut" -RedirectStandardError "$backendErr"
    if (-not (Wait-ForHttp -Uri 'http://localhost:8082/cinerama/api/v1/peliculas/activas' `
        -TimeoutSeconds 120 -Process $backendProcess -Component 'El backend')) {
        throw "El backend no respondió. Revisa los logs: $backendOut y $backendErr"
    }

    Write-Host 'Iniciando la web...'
    $frontendProcess = Start-Process -FilePath $node.Source `
        -ArgumentList ('"' + $ngCli + '" serve --port 4200') `
        -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput "$frontendOut" -RedirectStandardError "$frontendErr"
    if (-not (Wait-ForHttp -Uri 'http://localhost:4200' `
        -TimeoutSeconds 90 -Process $frontendProcess -Component 'El frontend')) {
        throw "La web no respondió. Revisa los logs: $frontendOut y $frontendErr"
    }

    Write-Host ''
    Write-Host 'Cinerama está listo: http://localhost:4200'
    Write-Host 'Backend: http://localhost:8082/cinerama'
    Write-Host "Logs: $backendOut ; $backendErr ; $frontendOut ; $frontendErr"
    Write-Host 'Oracle XE seguirá activo al cerrar Cinerama.'
    Start-Process 'http://localhost:4200'
    Read-Host 'Presiona Enter para cerrar Cinerama'
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host 'Presiona Enter para cerrar'
} finally {
    Stop-AppProcess -Process $frontendProcess
    Stop-AppProcess -Process $backendProcess
}
