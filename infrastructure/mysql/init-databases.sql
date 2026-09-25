-- =====================================================================
-- Hospitality Management System - Database Initialization Script
-- Database-per-Service Architecture: 10 Isolated Schemas
-- =====================================================================

-- 1. Authentication & Identity Service Database
CREATE DATABASE IF NOT EXISTS `hms_auth_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Customer Profile Service Database
CREATE DATABASE IF NOT EXISTS `hms_customer_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. Hotel Catalog & Search Service Database
CREATE DATABASE IF NOT EXISTS `hms_hotel_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 4. Room Inventory & Availability Service Database
CREATE DATABASE IF NOT EXISTS `hms_room_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 5. Booking & Reservation Orchestration Service Database
CREATE DATABASE IF NOT EXISTS `hms_booking_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 6. Food & Restaurant Service Database
CREATE DATABASE IF NOT EXISTS `hms_food_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 7. Room Service Management (Housekeeping/Amenities) Database
CREATE DATABASE IF NOT EXISTS `hms_rsm_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 8. Billing & Simulated Payment Service Database
CREATE DATABASE IF NOT EXISTS `hms_billing_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 9. Hotel Supplies & Inventory Service Database
CREATE DATABASE IF NOT EXISTS `hms_inventory_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 10. Notification Service Database
CREATE DATABASE IF NOT EXISTS `hms_notification_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================================
-- Application User Setup & Grant Privileges
-- =====================================================================
CREATE USER IF NOT EXISTS 'hms_user'@'%' IDENTIFIED BY 'hms_password';

-- Grant access on all HMS microservice schemas
GRANT ALL PRIVILEGES ON `hms_auth_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_customer_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_hotel_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_room_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_booking_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_food_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_rsm_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_billing_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_inventory_db`.* TO 'hms_user'@'%';
GRANT ALL PRIVILEGES ON `hms_notification_db`.* TO 'hms_user'@'%';

FLUSH PRIVILEGES;
