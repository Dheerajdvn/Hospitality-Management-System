# ==============================================================================
# Grand Luxe Hospitality Management System - Start All Services (PowerShell)
# ==============================================================================

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Starting Grand Luxe Hospitality Microservices Architecture" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan

$root = $PSScriptRoot

# 1. Verify Docker Infrastructure (PostgreSQL 16, Redis 7 & Apache Kafka KRaft)
Write-Host "`n[1/4] Verifying Infrastructure Containers..." -ForegroundColor Yellow
$pgContainer = docker ps --filter "name=hms-postgres" --format "{{.Status}}"
$redisContainer = docker ps --filter "name=hms-redis" --format "{{.Status}}"
$kafkaContainer = docker ps --filter "name=hms-kafka" --format "{{.Status}}"

if (-not $pgContainer -or -not $redisContainer -or -not $kafkaContainer) {
    Write-Host "Starting Docker infrastructure via docker-compose..." -ForegroundColor Yellow
    docker compose -f "$root/infrastructure/docker/docker-compose-infra.yml" up -d
    Start-Sleep -Seconds 8

    # Initialize Kafka topics
    if (Test-Path "$root/infrastructure/kafka/init-topics.ps1") {
        Write-Host "Initializing Kafka canonical event topics..." -ForegroundColor Cyan
        & "$root/infrastructure/kafka/init-topics.ps1"
    }
} else {
    Write-Host " - PostgreSQL 16 (Port 5432): RUNNING" -ForegroundColor Green
    Write-Host " - Redis 7 (Port 6379):       RUNNING" -ForegroundColor Green
    Write-Host " - Kafka 3.7 KRaft (Port 9092): RUNNING" -ForegroundColor Green
}

# 2. Launch Netflix Eureka Discovery Server (:8761)
Write-Host "`n[2/4] Launching Netflix Eureka Discovery Server (:8761)..." -ForegroundColor Yellow
$eurekaPort = Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue
if ($eurekaPort) {
    Write-Host " - eureka-server already listening on port :8761" -ForegroundColor Green
} else {
    Write-Host " - Starting eureka-server (:8761)..." -ForegroundColor Cyan
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k title HMS: eureka-server && mvn spring-boot:run" -WorkingDirectory "$root/backend/eureka-server" -WindowStyle Minimized
    Write-Host " - Waiting for eureka-server to initialize on port :8761..." -ForegroundColor DarkGray
    $retries = 0
    while (-not (Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue) -and $retries -lt 30) {
        Start-Sleep -Seconds 1
        $retries++
    }
    if (Get-NetTCPConnection -LocalPort 8761 -ErrorAction SilentlyContinue) {
        Write-Host " - eureka-server is UP and listening on :8761" -ForegroundColor Green
    }
}

# 3. Launch Backend Microservices
Write-Host "`n[3/4] Launching Java 21 / Spring Boot 3 Microservices in Background..." -ForegroundColor Yellow

# Core business microservices
$coreServices = @(
    @{ Name = "auth-service"; Port = 8081; Dir = "backend/auth-service" },
    @{ Name = "customer-service"; Port = 8082; Dir = "backend/customer-service" },
    @{ Name = "hotel-service"; Port = 8083; Dir = "backend/hotel-service" },
    @{ Name = "room-service"; Port = 8084; Dir = "backend/room-service" },
    @{ Name = "booking-service"; Port = 8085; Dir = "backend/booking-service" },
    @{ Name = "food-service"; Port = 8086; Dir = "backend/food-service" },
    @{ Name = "room-service-management"; Port = 8087; Dir = "backend/room-service-management" },
    @{ Name = "billing-service"; Port = 8088; Dir = "backend/billing-service" },
    @{ Name = "inventory-service"; Port = 8089; Dir = "backend/inventory-service" },
    @{ Name = "notification-service"; Port = 8090; Dir = "backend/notification-service" }
)

foreach ($svc in $coreServices) {
    $portActive = Get-NetTCPConnection -LocalPort $svc.Port -ErrorAction SilentlyContinue
    if ($portActive) {
        Write-Host " - $($svc.Name) already listening on port :$($svc.Port)" -ForegroundColor Green
    } else {
        Write-Host " - Starting $($svc.Name) (: $($svc.Port))..." -ForegroundColor Cyan
        Start-Process -FilePath "cmd.exe" -ArgumentList "/k title HMS: $($svc.Name) && mvn spring-boot:run" -WorkingDirectory "$root/$($svc.Dir)" -WindowStyle Minimized
        Start-Sleep -Milliseconds 800
    }
}

# Launch API Gateway Reverse Proxy (:8080)
$gatewayPort = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($gatewayPort) {
    Write-Host " - api-gateway already listening on port :8080" -ForegroundColor Green
} else {
    Write-Host " - Starting api-gateway (:8080)..." -ForegroundColor Cyan
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k title HMS: api-gateway && mvn spring-boot:run" -WorkingDirectory "$root/backend/api-gateway" -WindowStyle Minimized
}

# 4. Launch Frontend Portal
Write-Host "`n[4/4] Launching React 18 / Vite Frontend Portal..." -ForegroundColor Yellow
$frontendPort = Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
if (-not $frontendPort) {
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k title HMS: Frontend-UI && npm run dev -- --host 127.0.0.1 --port 5173" -WorkingDirectory "$root/frontend/hospitality-ui" -WindowStyle Minimized
    Write-Host " - Frontend launched on http://127.0.0.1:5173" -ForegroundColor Green
} else {
    Write-Host " - Frontend already active on http://127.0.0.1:5173" -ForegroundColor Green
}

Write-Host "`n======================================================================" -ForegroundColor Green
Write-Host " SYSTEM READY! Access Portal: http://localhost:5173" -ForegroundColor Yellow
Write-Host " API Gateway Entry:          http://localhost:8080" -ForegroundColor Cyan
Write-Host " Eureka Discovery Dashboard: http://localhost:8761" -ForegroundColor Magenta
Write-Host " Default Admin:              admin / admin123" -ForegroundColor White
Write-Host " Default Guest:              customer / customer123" -ForegroundColor White
Write-Host "======================================================================" -ForegroundColor Green
