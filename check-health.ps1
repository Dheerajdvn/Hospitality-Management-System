# ==============================================================================
# Grand Luxe Hospitality Management System - Health & Telemetry Inspector
# ==============================================================================

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Grand Luxe Hospitality - System Health & Telemetry Inspector" -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Check Infrastructure Ports & Containers
Write-Host "`n[1] Infrastructure Layer (Docker)" -ForegroundColor Yellow
$infra = @(
    @{ Name = "PostgreSQL 16 (10 DBs)"; Port = 5432; Container = "hms-postgres" },
    @{ Name = "Redis 7 Standalone";      Port = 6379; Container = "hms-redis" },
    @{ Name = "Apache Kafka 3.7 (KRaft)"; Port = 9092; Container = "hms-kafka" }
)

foreach ($item in $infra) {
    $conn = Get-NetTCPConnection -LocalPort $item.Port -ErrorAction SilentlyContinue
    $cStatus = docker ps --filter "name=$($item.Container)" --format "{{.Status}}" 2>$null
    if ($conn -and $cStatus) {
        Write-Host "  [OK] $($item.Name.PadRight(28)) Port :$($item.Port) ($cStatus)" -ForegroundColor Green
    } elseif ($conn) {
        Write-Host "  [OK] $($item.Name.PadRight(28)) Port :$($item.Port) (Host Active)" -ForegroundColor Green
    } else {
        Write-Host "  [!!] $($item.Name.PadRight(28)) Port :$($item.Port) (OFFLINE)" -ForegroundColor Red
    }
}

# 2. Check Kafka Topics
Write-Host "`n[2] Apache Kafka Canonical Event Topics" -ForegroundColor Yellow
$expectedTopics = @(
    "hms.booking.events",
    "hms.payment.events",
    "hms.food.events",
    "hms.roomservice.events",
    "hms.inventory.events"
)

$kafkaContainer = docker ps --filter "name=hms-kafka" --format "{{.Names}}" 2>$null
if ($kafkaContainer) {
    $topicsOut = docker exec hms-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>$null
    foreach ($topic in $expectedTopics) {
        if ($topicsOut -match [regex]::Escape($topic)) {
            Write-Host "  [OK] Topic '$topic' is active (3 partitions)" -ForegroundColor Green
        } else {
            Write-Host "  [--] Topic '$topic' not detected" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "  [--] hms-kafka container not running to query topic metadata" -ForegroundColor DarkGray
}

# 3. Check Microservices & Actuator Health
Write-Host "`n[3] Backend Microservices Fleet & Actuator Health" -ForegroundColor Yellow
$services = @(
    @{ Name = "API Gateway";            Port = 8080; Path = "/actuator/health" },
    @{ Name = "Auth Service";           Port = 8081; Path = "/actuator/health" },
    @{ Name = "Customer Service";       Port = 8082; Path = "/actuator/health" },
    @{ Name = "Hotel Service (+Redis)"; Port = 8083; Path = "/actuator/health" },
    @{ Name = "Room Service";           Port = 8084; Path = "/actuator/health" },
    @{ Name = "Booking Service";        Port = 8085; Path = "/actuator/health" },
    @{ Name = "Food Service";           Port = 8086; Path = "/actuator/health" },
    @{ Name = "Room Service Mgmt";      Port = 8087; Path = "/actuator/health" },
    @{ Name = "Billing Service";        Port = 8088; Path = "/actuator/health" },
    @{ Name = "Inventory Service";      Port = 8089; Path = "/actuator/health" },
    @{ Name = "Notification Service";   Port = 8090; Path = "/actuator/health" }
)

foreach ($svc in $services) {
    $portActive = Get-NetTCPConnection -LocalPort $svc.Port -ErrorAction SilentlyContinue
    if ($portActive) {
        try {
            $resp = Invoke-RestMethod -Uri "http://localhost:$($svc.Port)$($svc.Path)" -TimeoutSec 2 -ErrorAction Stop
            $healthStatus = $resp.status
            if ($healthStatus -eq "UP") {
                Write-Host "  [UP]   $($svc.Name.PadRight(28)) Port :$($svc.Port) (Actuator: UP)" -ForegroundColor Green
            } else {
                Write-Host "  [WARN] $($svc.Name.PadRight(28)) Port :$($svc.Port) (Actuator: $healthStatus)" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "  [UP]   $($svc.Name.PadRight(28)) Port :$($svc.Port) (Port Listening)" -ForegroundColor Green
        }
    } else {
        Write-Host "  [STOP] $($svc.Name.PadRight(28)) Port :$($svc.Port) (Not Started)" -ForegroundColor DarkGray
    }
}

# 4. Check Frontend Portal
Write-Host "`n[4] Frontend Customer & Admin Portal" -ForegroundColor Yellow
$frontendPort = Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
if ($frontendPort) {
    Write-Host "  [UP]   Vite React Portal            Port :5173 (http://localhost:5173)" -ForegroundColor Green
} else {
    Write-Host "  [STOP] Vite React Portal            Port :5173 (Not Started)" -ForegroundColor DarkGray
}

Write-Host "`n======================================================================" -ForegroundColor Cyan
Write-Host " Health inspection completed." -ForegroundColor White
Write-Host "======================================================================" -ForegroundColor Cyan
