# Database Architecture & Schema Isolation Guide (PostgreSQL 16)

## 1. Database-per-Service Principle

In this microservices architecture, each service strictly encapsulates its own relational data store.
We use **PostgreSQL 16** (pre-downloaded locally in Docker Desktop):

### 10 Isolated Databases on PostgreSQL 16
The single local PostgreSQL 16 instance hosts 10 logically and physically isolated databases:
- `hms_auth_db` — Authentication, Users, Roles, BCrypt Passwords
- `hms_customer_db` — Customer Profiles & Contact Details
- `hms_hotel_db` — Hotels, Amenities, Catalog
- `hms_room_db` — Room inventory, Room types, Room status
- `hms_booking_db` — Reservations, Booking items, Room date locks
- `hms_food_db` — Restaurant menus & Food orders
- `hms_rsm_db` — Room service requests & Housekeeping
- `hms_billing_db` — Aggregated invoices & Simulated payment transactions
- `hms_inventory_db` — Hotel supplies & Consumables
- `hms_notification_db` — In-app notifications & Event history

### Why Separate Databases in PostgreSQL?
In PostgreSQL, creating separate databases (`CREATE DATABASE hms_auth_db`, etc.) guarantees **strict boundary isolation**:
1. **Zero Cross-Database Joins**: PostgreSQL does not allow cross-database SQL queries in standard queries. This physically prevents developers from writing monolithic queries that join `booking` tables with `customer` tables.
2. **Dedicated Connection Pools**: Each Spring Boot service maintains its own HikariCP connection pool directed to its specific database (`jdbc:postgresql://localhost:5432/hms_auth_db`).
3. **Independent Cloud Migration**: In production, any database can be migrated to its own AWS RDS PostgreSQL instance without any code change.

## 2. Credentials & Connection Details
- **Host**: `localhost` (Port: `5432`)
- **Application User**: `hms_user`
- **Application Password**: `hms_password`
- **Superuser**: `postgres` / `postgres`

## 3. Spring Boot JPA & Hibernate Configuration
In each Spring Boot service's `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hms_auth_db
    username: hms_user
    password: hms_password
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
```
