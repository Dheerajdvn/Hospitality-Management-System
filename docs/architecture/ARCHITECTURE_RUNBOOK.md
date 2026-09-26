# Grand Luxe Hospitality Management System — Architecture & Operations Runbook

## 1. System Overview & Architectural Mandate

Grand Luxe is an enterprise-grade, distributed microservices hospitality platform built to demonstrate production-grade distributed systems patterns expected of a senior (3–4+ years experience) Java backend developer.

### Key Architectural Pillars
- **Database-per-Service**: 10 decoupled business microservices, each with its own dedicated PostgreSQL schema/database. Services communicate strictly via REST (OpenFeign) or asynchronous event streams (Kafka). No cross-database joins or shared tables.
- **Service Registry & Discovery**: Spring Cloud Netflix Eureka Server (port 8761) manages runtime registration and health heartbeats. All services register as Eureka clients, allowing API Gateway and Feign to dynamically route traffic using `lb://<service-name>` virtual URLs.
- **Event-Driven Architecture (EDA)**: Apache Kafka (KRaft mode, port 9092) with 5 canonical topics handling core lifecycle events: Booking Created/Expired, Payment Settled/Failed, Room Service Requested, and Inventory Stock Movement/Low-Stock Alerts.
- **Edge Security & Gateway Routing**: Spring Cloud Gateway (Netty Reactive, port 8080) validates HS512 JWTs, injects verified downstream identity headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`, `X-User-Role`, `X-User-Name`), applies global CORS for the Vite React UI, and load-balances via Eureka discovery.
- **Cache-Aside Pattern**: Redis 7 on port 6379 caching hot hotel catalog queries with Jackson polymorphic serialization and TTL invalidation on hotel metadata updates.
- **Optimistic Concurrency & Double-Booking Prevention**: Room inventory protected by Hibernate `@Version` optimistic locking combined with atomic mathematical date overlap SQL queries (`checkIn < :checkOut AND checkOut > :checkIn`).
- **DevOps Tooling & Observability**: Integrated `spring-boot-devtools` for live reloading and `micrometer-registry-prometheus` exposing metrics at `/actuator/prometheus` on all microservices.

---

## 2. Microservice Fleet Registry

| Service Name | Port | Database / Engine | Key Architectural Responsibility |
| :--- | :---: | :--- | :--- |
| **eureka-server** | `8761` | In-Memory Registry | Netflix Eureka service discovery server, instance registration & heartbeats |
| **api-gateway** | `8080` | Netty / Reactive WebFlux | Perimeter reverse proxy, JWT verification, CORS policy, Eureka dynamic `lb://` routing |
| **auth-service** | `8081` | PostgreSQL (`hms_auth_db`) | RBAC, BCrypt password hashing, 512-bit HS512 JWT token issuance |
| **customer-service** | `8082` | PostgreSQL (`hms_customer_db`) | Guest profiles, KYC validation endpoint via Feign |
| **hotel-service** | `8083` | PostgreSQL (`hms_hotel_db`) + Redis 7 | Hotel catalog, JPA Specifications, Redis Cache-Aside layer |
| **room-service** | `8084` | PostgreSQL (`hms_room_db`) | Room inventory, `@Version` optimistic locking, room search |
| **booking-service** | `8085` | PostgreSQL (`hms_booking_db`) | Date overlap query, 15-min hold sweep, Payment event consumer |
| **food-service** | `8086` | PostgreSQL (`hms_food_db`) | Dining catalog, batch food item resolution endpoint (`/batch`) |
| **room-service-mgmt** | `8087` | PostgreSQL (`hms_rsm_db`) | In-room dining orders, KOT state machine, immutable price snapshots |
| **billing-service** | `8088` | PostgreSQL (`hms_billing_db`) | 18% GST calculation, dual event/Feign payment flow, invoice generation |
| **inventory-service** | `8089` | PostgreSQL (`hms_inventory_db`) | Double-entry stock ledger, automated KOT stock deduction, reorder alerts |
| **notification-service** | `8090` | PostgreSQL (`hms_notification_db`) | Multi-channel simulated email/SMS dispatch with audit logging |
| **hospitality-ui** | `5173` | React 18 / Vite / Vanilla CSS | Luxury guest reservation portal & operations admin dashboard |

---

## 3. Apache Kafka Event-Driven Architecture

### 3.1 Broker Specification
- **Engine**: Apache Kafka 3.7.0 running in **KRaft mode** (no ZooKeeper dependency).
- **Port**: `localhost:9092`
- **Default Partitions**: 3 partitions per topic with Replication Factor 1 (local development).

### 3.2 Canonical Topic Catalog

| Topic Name | Message Key | Producer Service | Consumer Service(s) | Business Trigger |
| :--- | :--- | :--- | :--- | :--- |
| `hms.booking.events` | `bookingId` | `booking-service` | `billing-service`<br/>`notification-service` | Reservation hold placed; reservation expired |
| `hms.payment.events` | `bookingId` | `billing-service` | `booking-service`<br/>`notification-service` | Payment completed (`PAID`); payment failed |
| `hms.food.events` | `orderId` | `food-service` | `billing-service`<br/>`inventory-service` | Menu item price or availability change |
| `hms.roomservice.events` | `bookingId` | `room-service-mgmt` | `inventory-service`<br/>`billing-service`<br/>`notification-service` | In-room dining order placed (`KOT` generated) |
| `hms.inventory.events` | `itemCode` | `inventory-service` | `notification-service` | Stock updated; item breaches reorder threshold |

### 3.3 Partitioning & Ordering Guarantee
All payment and booking lifecycle events use `bookingId` as the Kafka message key. Because Kafka routes messages with identical keys to the same partition, chronological sequencing (`BookingCreated` $\to$ `PaymentCompleted` $\to$ `BookingConfirmed`) is strictly preserved per reservation.

---

## 4. DevOps Tooling & Observability

### 4.1 Spring Boot DevTools
All 11 microservice `pom.xml` files include:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```
- Provides live reload and automatic restart on class compilation.
- Disables template and static caching during local development.

### 4.2 Prometheus & Micrometer Actuator
All microservices include:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```
- Exposes standard OpenMetrics/Prometheus format metrics at `/actuator/prometheus`.
- Provides JVM memory, GC pauses, HikariCP connection pool stats, and HTTP request metrics for DevOps ingestion.

### 4.3 Health Check Inspector
A dedicated PowerShell script `check-health.ps1` verifies the health of all tiers in seconds:
```powershell
.\check-health.ps1
```
Checks:
1. Docker container status (`hms-postgres`, `hms-redis`, `hms-kafka`).
2. Kafka topic registration (verifies 5 topics inside the `hms-kafka` container).
3. Eureka Discovery Server health (`http://localhost:8761`).
4. Microservices Actuator health (`/actuator/health` on ports 8080–8090).
5. Frontend portal accessibility on port 5173.

---

## 5. Local Execution & Lifecycle Commands

### Starting the Platform
```powershell
# 1. Start Docker Infrastructure (Postgres, Redis, Kafka)
docker compose -f infrastructure/docker/docker-compose-infra.yml up -d

# 2. Launch All Microservices, Eureka Server & Frontend
.\start-all.ps1
```

### Access Points
- **Guest & Admin Portal**: `http://localhost:5173`
- **Eureka Discovery Dashboard**: `http://localhost:8761`
- **API Gateway Entry**: `http://localhost:8080`

### Inspecting Health
```powershell
.\check-health.ps1
```

### Stopping the Platform
```powershell
# Stop backend microservices, gateway, eureka & frontend:
.\stop-all.ps1

# Stop everything including Docker containers:
.\stop-all.ps1 -StopInfra
```

---

## 6. Senior Java Developer Interview Highlights

### Q1: How is the double-booking problem solved?
**Answer**: Through a two-layer defense mechanism:
1. **Mathematical Date Overlap Query**: When reserving a room, the booking repository executes an overlap check:
   ```sql
   SELECT COUNT(b) FROM Booking b
   WHERE b.roomId = :roomId
     AND b.status IN ('CONFIRMED', 'PENDING_PAYMENT')
     AND b.checkInDate < :checkOutDate
     AND b.checkOutDate > :checkInDate
   ```
2. **Hibernate Concurrency Control**: The `Room` entity uses `@Version private Long version;` for optimistic locking. If two concurrent transactions attempt to hold or book the room simultaneously, Hibernate detects version divergence and throws `OptimisticLockException`, preventing race conditions.

### Q2: Why use the Database-per-Service pattern instead of a shared database?
**Answer**:
- **Loose Coupling**: Services can modify schemas without breaking other domains.
- **Independent Scaling**: High-throughput services (such as room search or hotel catalog) can scale their database independently from less-frequent operations (such as customer profile updates).
- **Fault Isolation**: An outage or slow query in the dining or billing service does not exhaust database connection pools for core booking flows.

### Q3: How is inter-service security handled at the API Gateway?
**Answer**:
- The API Gateway acts as a perimeter reverse proxy running Spring Cloud Gateway on Netty WebFlux.
- Incoming requests with `Authorization: Bearer <JWT>` are intercepted by `JwtAuthenticationFilter` (order `-100`).
- The gateway verifies the HMAC-SHA-512 cryptographic signature, extracts claims (`userId`, `email`, `roles`), and injects downstream headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`, `X-User-Role`, `X-User-Name`).
- Downstream microservices do not need to re-verify cryptographic signatures on every internal hop; they simply read trusted headers.
- If a token is expired or missing, the gateway returns `401 Unauthorized` with `Access-Control-Allow-Origin` preserved, allowing frontends to display clear session-expired messages without browser CORS interference.

### Q4: Why is Kafka in KRaft mode preferred over ZooKeeper?
**Answer**:
- KRaft (Kafka Raft Metadata mode) replaces the separate ZooKeeper cluster with an integrated Raft consensus mechanism within the Kafka controllers.
- This eliminates metadata synchronization bottlenecks, reduces memory and container overhead, enables rapid failover, and represents the modern production standard for Apache Kafka (since Kafka 3.3+).

### Q5: Why use Spring Cloud Netflix Eureka for Service Discovery and dynamic routing?
**Answer**:
- **Dynamic Service Registration**: In elastic cloud and containerized environments, IP addresses and ports change dynamically. Hardcoding IPs/ports creates brittle configurations.
- **Client-Side Load Balancing**: Services and the API Gateway use `lb://<service-name>` virtual URIs. Spring Cloud LoadBalancer interrogates Eureka's local cache to distribute incoming requests across available instances with zero hardware load balancer overhead.
- **Self-Healing & Heartbeats**: Eureka clients send 30-second heartbeats (`eureka.client.service-url`). If an instance crashes or fails health checks, Eureka evicts it from the active registry, ensuring failover without manual intervention.
