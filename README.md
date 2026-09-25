# Grand Luxe — Hospitality Management System
### Production-Grade Microservices Architecture (Spring Boot 3.3.4, Java 21, PostgreSQL 16, Redis 7, React 18)

---

## 1. Overview

**Grand Luxe** is an enterprise-grade hospitality and hotel management platform built to showcase production patterns expected of a **3–4+ years experienced Java Backend Developer**.

The platform provides end-to-end capabilities:
- **Guest Portal**: Browse 5-star properties, check real-time suite availability, book rooms with mathematical overlap conflict prevention, order in-room dining with live kitchen order tickets (KOT), and download GST-compliant tax invoices.
- **Operations Portal**: Real-time room service order management, kitchen status progression, inventory stock movement ledger, and automated low-stock threshold alerting.
- **Microservices Architecture**: 10 decoupled business microservices + 1 Edge Reactive API Gateway built on the **Database-per-Service** pattern with PostgreSQL 16, Redis 7 caching, and OpenFeign inter-service communication.

---

## 2. Technology Stack

- **Backend**: Java 21, Spring Boot 3.3.4, Spring Cloud 2023.0.3, Spring Data JPA / Hibernate 6, Spring Cloud Gateway (Netty / WebFlux), Spring Security 6, JJWT 0.12.5 (HS512), OpenFeign with Apache HttpClient 5.
- **Persistence & Caching**: PostgreSQL 16 (10 isolated databases), Redis 7 Standalone (Cache-Aside pattern).
- **Messaging & EDA**: Apache Kafka 3.7 (KRaft mode, 5 canonical topics with 3 partitions each).
- **DevOps & Observability**: `spring-boot-devtools` (live-reload), `micrometer-registry-prometheus` (Prometheus metrics scraping via `/actuator/prometheus`), Spring Boot Actuator, PowerShell multi-tier health inspector.
- **Frontend**: React 18, Vite, Lucide Icons, Axios, Luxury Dark & Gold Glassmorphism Design System.
- **Testing**: JUnit 5, Mockito 5, Reactor Test.

---

## 3. Microservices Ports & Roles

All external client traffic routes through the **API Gateway on Port 8080**:

| Service | Port | Database / Engine | Key Architectural Responsibility |
|---|---|---|---|
| **api-gateway** | **8080** | Netty / Reactive WebFlux | Perimeter reverse proxy, JWT verification, CORS policy, user context header injection |
| **auth-service** | **8081** | PostgreSQL (`hms_auth_db`) | RBAC, BCrypt hashing, 512-bit HS512 JWT issuance |
| **customer-service** | **8082** | PostgreSQL (`hms_customer_db`) | Guest profiles, KYC validation endpoint |
| **hotel-service** | **8083** | PostgreSQL (`hms_hotel_db`) + Redis 7 | Hotel catalog, JPA Specifications, Redis Cache-Aside |
| **room-service** | **8084** | PostgreSQL (`hms_room_db`) | Room inventory, `@Version` optimistic locking |
| **booking-service** | **8085** | PostgreSQL (`hms_booking_db`) | Date overlap query, 15-minute temporary hold sweep, Payment event consumer |
| **food-service** | **8086** | PostgreSQL (`hms_food_db`) | Dining catalog, high-performance batch item lookup (`/batch`) |
| **room-service-mgmt**| **8087** | PostgreSQL (`hms_rsm_db`) | In-room dining orders, KOT state machine, price snapshots, Kafka publisher |
| **billing-service** | **8088** | PostgreSQL (`hms_billing_db`) | 18% GST calculation, simulated payment gateway, dual Kafka/Feign flow |
| **inventory-service**| **8089** | PostgreSQL (`hms_inventory_db`) | Hotel supplies, double-entry stock ledger, reorder alerts, automated KOT stock deduction |
| **notification-service**|**8090** | PostgreSQL (`hms_notification_db`)| Multi-channel email/SMS dispatch with audit logging |
| **hospitality-ui** | **5173** | React 18 / Vite | Single-page luxury customer reservation portal & operations admin dashboard |

---

## 4. Apache Kafka Canonical Event Topics

The platform uses **Apache Kafka 3.7.0 in KRaft mode** (port 9092) with 5 canonical topics:

| Topic Name | Partitions | Message Key | Producers | Consumers |
|---|---|---|---|---|
| `hms.booking.events` | 3 | `bookingId` | `booking-service` | `billing-service`, `notification-service` |
| `hms.payment.events` | 3 | `bookingId` | `billing-service` | `booking-service`, `notification-service` |
| `hms.food.events` | 3 | `orderId` | `food-service` | `billing-service`, `inventory-service` |
| `hms.roomservice.events` | 3 | `bookingId` | `room-service-mgmt` | `inventory-service`, `billing-service`, `notification-service` |
| `hms.inventory.events` | 3 | `itemCode` | `inventory-service` | `notification-service` |

---

## 5. Documentation Links

- **[System Architecture & Operations Runbook](docs/architecture/ARCHITECTURE_RUNBOOK.md)**: Master operational manual, service topology, Kafka event schemas, and senior Java interview Q&A.
- **[High-Level Workflow Design (HWD)](docs/architecture/HWD_WORKFLOW.md)**: End-to-end business workflows, microservices topology, database-per-service boundaries, Redis cache-aside, and sequence diagrams.
- **[Low-Level Workflow Design (LWD)](docs/architecture/LWD_WORKFLOW.md)**: Detailed ER diagrams, state machines (Booking, KOT), mathematical date overlap proof, optimistic locking, and complete REST API contracts.
- **[Kafka Event Topology Specification](docs/kafka/kafka-design.md)**: Detailed event streaming architecture, KRaft quorum details, and partitioning guarantees.
- **[Resilience4j & Actuator Fault Tolerance Guide](docs/interview/RESILIENCE4J_GUIDE.md)**: Circuit breaker state machine, exponential backoff retries, Spring AOP proxy delegates, and live Actuator health monitoring.
- **[System Architecture Reference Manual](docs/architecture/ARCHITECTURE.md)**: System topology, database-per-service mapping, communication matrix, and sequence diagrams.
- **[Java Backend Developer Interview Playbook](docs/interview/INTERVIEW_PLAYBOOK.md)**: 25+ comprehensive interview questions & model answers covering Spring Boot 3, Hibernate concurrency, Redis caching, Kafka, and distributed consistency.

---

## 6. Quickstart & Lifecycle Commands

### Step 1: Start Infrastructure (Docker)
Ensure Docker Desktop is running, then launch PostgreSQL 16, Redis 7, and Kafka 3.7 KRaft:
```bash
docker compose -f infrastructure/docker/docker-compose-infra.yml up -d
```

### Step 2: One-Click Startup
To launch all 11 backend microservices and the React frontend:

**On Windows (PowerShell):**
```powershell
.\start-all.ps1
```

**On Linux / macOS (Bash):**
```bash
chmod +x start-all.sh stop-all.sh
./start-all.sh
```

### Step 3: Multi-Tier Health Inspection
To verify all infrastructure containers, Kafka topics, Actuator endpoints, and the frontend in one command:
```powershell
.\check-health.ps1
```

### Step 4: Access the Application
- **Frontend Portal**: [http://localhost:5173](http://localhost:5173)
- **API Gateway Entry**: [http://localhost:8080](http://localhost:8080)
- **Default Admin Account**: `admin` / `admin123`
- **Default Guest Account**: `customer` / `customer123`

### Step 5: Stop Services
```powershell
# Stop microservices and frontend:
.\stop-all.ps1

# Stop everything including Docker containers:
.\stop-all.ps1 -StopInfra
```

---

## 7. Key Design Decisions

1. **Edge Authentication & Header Mutation**: The Gateway intercepts incoming traffic, validates JWT signatures, and injects trusted `X-User-Id`, `X-User-Email`, `X-User-Roles`, `X-User-Role`, and `X-User-Name` headers downstream, eliminating redundant cryptographic parsing in downstream services.
2. **Double-Booking Prevention**: Mathematical date overlap query (`checkIn < :newCheckOut AND checkOut > :newCheckIn`) inside `@Transactional` blocks, backed by Hibernate `@Version` optimistic locking on rooms.
3. **Partitioned Ordering in Kafka**: Payment and booking events use `bookingId` as the message key to guarantee strict chronological event sequencing per reservation.
4. **Immutability in Dining Orders**: In-room dining line items store frozen snapshots of `unitPrice` and `foodItemName` at the moment of order placement to guarantee historical financial accuracy.
5. **N+1 Prevention**: Room Service Management uses Food Service's `POST /api/v1/food/batch` endpoint to resolve entire dining carts in a single round-trip.
6. **Windows Timezone Resilience**: Configured UTC timezones in every service's Hikari connection properties and static initializer blocks to permanently resolve the Windows `Asia/Calcutta` JDBC driver defect.
