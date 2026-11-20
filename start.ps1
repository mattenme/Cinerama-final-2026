# Arranca Cinerama completo: backend (8082) y frontend (4200).
# Uso: powershell -ExecutionPolicy Bypass -File start.ps1
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Definition
$logBackend = Join-Path $env:TEMP 'cinerama-backend.log'
$logFrontend = Join-Path $env:TEMP 'cinerama-frontend.log'

Write-Host '== Cinerama ==' -ForegroundColor Cyan

if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Falta Java 21 en el PATH.' }
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Falta Node (18+) en el PATH.' }

Write-Host "-> Backend  http://localhost:8082/cinerama  (log: $logBackend)"
$backend = Start-Process -FilePath 'cmd.exe' `
    -ArgumentList '/c', 'mvnw.cmd -DskipTests spring-boot:run > "' + $logBackend + '" 2>&1' `
    -WorkingDirectory (Join-Path $raiz 'backend') -PassThru -WindowStyle Hidden

Write-Host "-> Frontend http://localhost:4200           (log: $logFrontend)"
$frontendDir = Join-Path $raiz 'frontend'
if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
    Write-Host '   Instalando dependencias del frontend...'
    Push-Location $frontendDir
    npm install | Out-Null
    Pop-Location
}
$frontend = Start-Process -FilePath 'cmd.exe' `
    -ArgumentList '/c', 'node_modules\.bin\ng.cmd serve --port 4200 > "' + $logFrontend + '" 2>&1' `
    -WorkingDirectory $frontendDir -PassThru -WindowStyle Hidden

Write-Host 'Esperando a que el backend responda...'
for ($i = 0; $i -lt 60; $i++) {
    try {
        Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 `
            'http://localhost:8082/cinerama/api/v1/peliculas/activas' | Out-Null
        Write-Host 'Backend listo.' -ForegroundColor Green
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}

Start-Process 'http://localhost:4200'
Write-Host ''
Write-Host 'Cuentas demo: 00000001/admin123 · 11223344/empleado123 · 77889901/123456'
Write-Host "PIDs: backend $($backend.Id), frontend $($frontend.Id)"
Write-Host "Para detenerlos: Stop-Process -Id $($backend.Id),$($frontend.Id) -Force"
