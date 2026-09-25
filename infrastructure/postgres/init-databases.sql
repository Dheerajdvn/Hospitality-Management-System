-- =====================================================================
-- Hospitality Management System - PostgreSQL Database Initialization
-- =====================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'hms_user') THEN
        CREATE ROLE hms_user WITH LOGIN PASSWORD 'hms_password' SUPERUSER CREATEDB;
    END IF;
END
$$;

SELECT 'CREATE DATABASE hms_auth_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_auth_db')\gexec
SELECT 'CREATE DATABASE hms_customer_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_customer_db')\gexec
SELECT 'CREATE DATABASE hms_hotel_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_hotel_db')\gexec
SELECT 'CREATE DATABASE hms_room_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_room_db')\gexec
SELECT 'CREATE DATABASE hms_booking_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_booking_db')\gexec
SELECT 'CREATE DATABASE hms_food_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_food_db')\gexec
SELECT 'CREATE DATABASE hms_rsm_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_rsm_db')\gexec
SELECT 'CREATE DATABASE hms_billing_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_billing_db')\gexec
SELECT 'CREATE DATABASE hms_inventory_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_inventory_db')\gexec
SELECT 'CREATE DATABASE hms_notification_db OWNER hms_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hms_notification_db')\gexec
