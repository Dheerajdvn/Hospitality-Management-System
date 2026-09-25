#!/bin/bash
set -e

# =====================================================================
# Hospitality Management System - PostgreSQL Database Initialization
# Creates dedicated application user and 10 isolated microservice databases
# =====================================================================

echo "Creating HMS application user..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'hms_user') THEN
            CREATE ROLE hms_user WITH LOGIN PASSWORD 'hms_password' SUPERUSER CREATEDB;
        END IF;
    END
    \$\$;
EOSQL

DATABASES=(
    "hms_auth_db"
    "hms_customer_db"
    "hms_hotel_db"
    "hms_room_db"
    "hms_booking_db"
    "hms_food_db"
    "hms_rsm_db"
    "hms_billing_db"
    "hms_inventory_db"
    "hms_notification_db"
)

for db in "${DATABASES[@]}"; do
    echo "Provisioning isolated database: ${db}..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        SELECT 'CREATE DATABASE ${db} OWNER hms_user'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${db}')\gexec
        GRANT ALL PRIVILEGES ON DATABASE ${db} TO hms_user;
EOSQL
done

echo "All 10 HMS microservice databases successfully provisioned."
