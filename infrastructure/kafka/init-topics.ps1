# =====================================================================
# Hospitality Management System - Kafka Topic Initialization (PowerShell)
# =====================================================================

param (
    [string]$BootstrapServer = "localhost:9092"
)

Write-Host "Creating HMS Kafka Topics on broker: $BootstrapServer" -ForegroundColor Cyan

$topics = @(
    @{ Name = "hms.booking.events"; Partitions = 3; Replication = 1 },
    @{ Name = "hms.payment.events"; Partitions = 3; Replication = 1 },
    @{ Name = "hms.food.events"; Partitions = 3; Replication = 1 },
    @{ Name = "hms.roomservice.events"; Partitions = 3; Replication = 1 },
    @{ Name = "hms.inventory.events"; Partitions = 3; Replication = 1 }
)

foreach ($t in $topics) {
    Write-Host "Creating topic: $($t.Name)..." -ForegroundColor Yellow
    docker exec hms-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 `
        --create --if-not-exists `
        --topic $t.Name `
        --partitions $t.Partitions `
        --replication-factor $t.Replication
}

Write-Host "`nListing all Kafka topics on broker:" -ForegroundColor Cyan
docker exec hms-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

Write-Host "All HMS topics initialized successfully." -ForegroundColor Green

