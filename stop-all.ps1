param(
    [switch]$StopInfra
)

# ==============================================================================
# Grand Luxe Hospitality Management System - Stop All Services (PowerShell)
# ==============================================================================

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Stopping Grand Luxe Hospitality Microservices & Frontend" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan

$ports = @(8761, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090, 5173)

foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($p in $pids) {
            try {
                Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
                Write-Host " - Terminated process $($p) listening on port :$($port)" -ForegroundColor Green
            } catch {
                Write-Host " - Could not terminate PID $($p): $_" -ForegroundColor Red
            }
        }
    } else {
        Write-Host " - Port :$port is already free." -ForegroundColor DarkGray
    }
}

Write-Host "`nAll application services have been stopped." -ForegroundColor Green

if ($StopInfra) {
    Write-Host "`nShutting down Docker infrastructure (PostgreSQL 16, Redis 7, Kafka KRaft)..." -ForegroundColor Yellow
    docker compose -f "$PSScriptRoot/infrastructure/docker/docker-compose-infra.yml" down
    Write-Host "Docker infrastructure stopped." -ForegroundColor Green
} else {
    Write-Host "Note: Docker infrastructure (PostgreSQL, Redis & Kafka KRaft) is still active." -ForegroundColor Cyan
    Write-Host "To shut down Docker containers too, run: .\stop-all.ps1 -StopInfra" -ForegroundColor DarkGray
}
