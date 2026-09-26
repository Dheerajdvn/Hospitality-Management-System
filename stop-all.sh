#!/usr/bin/env bash
# ==============================================================================
# Grand Luxe Hospitality Management System - Stop All Services (Bash)
# ==============================================================================

ports=(8761 8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 5173)

for port in "${ports[@]}"; do
    pid=$(lsof -ti :$port 2>/dev/null || true)
    if [ -n "$pid" ]; then
        echo "Terminating process $pid listening on port :$port..."
        kill -9 $pid 2>/dev/null || true
    fi
done

echo "All services stopped."
