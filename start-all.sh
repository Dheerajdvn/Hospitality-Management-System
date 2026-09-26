#!/usr/bin/env bash
# ==============================================================================
# Grand Luxe Hospitality Management System - Start All Services (Bash)
# ==============================================================================

set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Starting Grand Luxe Hospitality Architecture from $ROOT_DIR..."

# 1. Start Docker Infrastructure
docker compose -f "$ROOT_DIR/infrastructure/docker/docker-compose-infra.yml" up -d

# 2. Launch Backend Microservices
services=(
    "eureka-server:8761:backend/eureka-server"
    "auth-service:8081:backend/auth-service"
    "customer-service:8082:backend/customer-service"
    "hotel-service:8083:backend/hotel-service"
    "room-service:8084:backend/room-service"
    "booking-service:8085:backend/booking-service"
    "food-service:8086:backend/food-service"
    "room-service-management:8087:backend/room-service-management"
    "billing-service:8088:backend/billing-service"
    "inventory-service:8089:backend/inventory-service"
    "notification-service:8090:backend/notification-service"
    "api-gateway:8080:backend/api-gateway"
)

for entry in "${services[@]}"; do
    IFS=":" read -r name port dir <<< "$entry"
    echo "Starting $name on port :$port..."
    (cd "$ROOT_DIR/$dir" && nohup mvn spring-boot:run > "$ROOT_DIR/$name.log" 2>&1 &)
done

# 3. Launch Frontend
echo "Starting React Frontend on port :5173..."
(cd "$ROOT_DIR/frontend/hospitality-ui" && nohup npm run dev -- --host 127.0.0.1 --port 5173 > "$ROOT_DIR/frontend.log" 2>&1 &)

echo "All services launched! Access: http://localhost:5173"
