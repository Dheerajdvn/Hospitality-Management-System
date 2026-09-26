# High-Level Workflow Design (HWD) — Grand Luxe HMS
### Production-Grade Microservices Architecture System Workflow Document

---

## 1. System Vision & Architecture Overview

**Grand Luxe** is an enterprise-grade Hospitality Management System engineered using modern cloud-native Java patterns. The system is designed to simulate a real-world 5-star luxury hotel chain operating multiple properties, featuring real-time room reservations, billing & tax invoicing, in-room dining with live kitchen order tickets (KOT), staff inventory tracking, multi-channel guest notifications, and high-concurrency protection.

### Architectural Core Principles
1. **Database-per-Service**: Each microservice strictly owns its dedicated PostgreSQL 16 database. No cross-service database queries or joins are permitted.
2. **Dynamic Service Registry & Discovery**: Spring Cloud Netflix Eureka Server (port 8761) maintains active instance registries and heartbeats. API Gateway and OpenFeign route dynamically using `lb://<service-name>` virtual URIs.
3. **API Gateway as the Single Entry Point**: A Spring Cloud Gateway (Netty / Reactive WebFlux, port 8080) acts as the reverse proxy, validating 512-bit HS512 JWTs, dynamically resolving downstream instances from Eureka, and injecting downstream identity headers.
4. **Resilience & Fault Tolerance**: Synchronous inter-service calls (OpenFeign) are guarded by **Resilience4j Circuit Breakers**, **Exponential Backoff Retries**, and **Spring Boot Actuator** health telemetry.
5. **High Concurrency & Audit Integrity**: Concurrency conflicts are mitigated using mathematical date-overlap checks and JPA `@Version` optimistic locking. Audit logs record room status transitions and stock movements immutably.

---

## 2. High-Level Architecture Topology

```mermaid
graph TB
    subgraph ClientLayer["Client & Presentation Tier"]
        UI["React 18 SPA (Vite)<br/>Port: 5173<br/>• Guest Booking & Dining Portal<br/>• Admin Operations Kanban"]
    end

    subgraph DiscoveryLayer["Service Registry & Discovery Tier"]
        EUREKA["Spring Cloud Netflix Eureka Server<br/>Port: 8761<br/>• Service Registration<br/>• Heartbeat Health Checking<br/>• Dynamic Load Balancing Registry"]
    end

    subgraph GatewayLayer["Edge Security & Routing Tier"]
        GW["Spring Cloud Gateway<br/>Port: 8080 (Reactive Netty)<br/>• HS512 JWT Validation<br/>• Downstream Header Injection<br/>• Centralized CORS Policy<br/>• Dynamic lb:// Routing"]
    end

    subgraph ServiceFleet["Microservices Fleet Tier (Spring Boot 3.3.4 / Java 21)"]
        AUTH["auth-service<br/>Port: 8081"]
        CUST["customer-service<br/>Port: 8082"]
        HOTEL["hotel-service<br/>Port: 8083"]
        ROOM["room-service<br/>Port: 8084"]
        BOOK["booking-service<br/>Port: 8085"]
        FOOD["food-service<br/>Port: 8086"]
        RSM["room-service-mgmt<br/>Port: 8087"]
        BILL["billing-service<br/>Port: 8088"]
        INV["inventory-service<br/>Port: 8089"]
        NOTIF["notification-service<br/>Port: 8090"]
    end

    subgraph StorageTier["Isolated Data Tier (Database-per-Service)"]
        DB_AUTH[("hms_auth_db<br/>Postgres :5432")]
        DB_CUST[("hms_customer_db<br/>Postgres :5432")]
        DB_HOTEL[("hms_hotel_db<br/>Postgres :5432")]
        DB_ROOM[("hms_room_db<br/>Postgres :5432")]
        DB_BOOK[("hms_booking_db<br/>Postgres :5432")]
        DB_FOOD[("hms_food_db<br/>Postgres :5432")]
        DB_RSM[("hms_rsm_db<br/>Postgres :5432")]
        DB_BILL[("hms_billing_db<br/>Postgres :5432")]
        DB_INV[("hms_inventory_db<br/>Postgres :5432")]
        DB_NOTIF[("hms_notification_db<br/>Postgres :5432")]
        CACHE[("Redis 7<br/>Port: 6379<br/>Hotel Catalog Cache")]
    end

    UI -->|HTTP / JSON| GW
    GW -.->|Fetch Registry| EUREKA
    ServiceFleet -.->|Heartbeat Registration| EUREKA

    GW ==>|lb://auth-service| AUTH
    GW ==>|lb://customer-service| CUST
    GW ==>|lb://hotel-service| HOTEL
    GW ==>|lb://room-service| ROOM
    GW ==>|lb://booking-service| BOOK
    GW ==>|lb://food-service| FOOD
    GW ==>|lb://room-service-management| RSM
    GW ==>|lb://billing-service| BILL
    GW ==>|lb://inventory-service| INV
    GW ==>|lb://notification-service| NOTIF

    AUTH --> DB_AUTH
    CUST --> DB_CUST
    HOTEL --> DB_HOTEL
    HOTEL --> CACHE
    ROOM --> DB_ROOM
    BOOK --> DB_BOOK
    FOOD --> DB_FOOD
    RSM --> DB_RSM
    BILL --> DB_BILL
    INV --> DB_INV
    NOTIF --> DB_NOTIF
```

---

## 3. Database-per-Service Mapping

| Microservice | Port | Database Name | Primary Domain Responsibility |
| :--- | :--- | :--- | :--- |
| **eureka-server** | `8761` | *In-Memory Registry* | Spring Cloud Netflix Eureka Discovery Server, dynamic service registration & heartbeat health tracking |
| **api-gateway** | `8080` | *None (Stateless)* | Netty reverse proxy, dynamic `lb://` route resolution, JWT auth filter, CORS |
| **auth-service** | `8081` | `hms_auth_db` | User credentials, BCrypt hashes, roles (`ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_STAFF`), JJWT token generator |
| **customer-service** | `8082` | `hms_customer_db` | Guest profiles, KYC document tracking, contact info, active status |
| **hotel-service** | `8083` | `hms_hotel_db` | Hotel property catalog, star ratings, amenities, Redis cache-aside |
| **room-service** | `8084` | `hms_room_db` | Room inventory, types (`DELUXE`, `SUITE`, `PRESIDENTIAL`), `@Version` locking, `RoomStatusLog` audit |
| **booking-service** | `8085` | `hms_booking_db` | Reservation state machine, date-overlap query, 15-min temporary holds, expiry scheduler |
| **food-service** | `8086` | `hms_food_db` | Dining catalog, dietary flags (`VEG`, `NON_VEG`, `JAIN`), batch item lookup |
| **room-service-management** | `8087` | `hms_rsm_db` | In-room dining orders, historical price snapshotting, KOT numbering, prep time tracking |
| **billing-service** | `8088` | `hms_billing_db` | Invoicing, 18% GST tax calculation (CGST 9% + SGST 9%), payment gateway simulation, card-failure triggers |
| **inventory-service** | `8089` | `hms_inventory_db` | Hotel assets (`LINEN`, `TOILETRIES`, `PANTRY`), immutable `StockMovementLog`, low-stock auto-alerts |
| **notification-service** | `8090` | `hms_notification_db` | Multi-channel dispatch (HTML email & SMS templates), dispatch audit log, async Kafka consumer |

---

## 4. End-to-End High-Level Workflows

### Workflow 1: Authentication & Token Propagation Flow
```mermaid
sequenceDiagram
    autonumber
    actor Guest as Guest / Admin
    participant UI as React UI (:5173)
    participant GW as API Gateway (:8080)
    participant Auth as Auth Service (:8081)
    participant DB as hms_auth_db
    participant Service as Downstream Microservice

    Guest->>UI: Enters username & password
    UI->>GW: POST /api/v1/auth/login
    GW->>Auth: Forward to Auth Service
    Auth->>DB: Query user by username
    DB-->>Auth: Return User entity with BCrypt hash
    Auth->>Auth: Verify password via PasswordEncoder
    Auth->>Auth: Generate JJWT (HS512, 24h validity, Claims: id, email, roles)
    Auth-->>GW: HTTP 200 { token, userDetails }
    GW-->>UI: Return JWT to browser (stored in localStorage)

    Note over UI,Service: Subsequent Authenticated Requests
    UI->>GW: GET /api/v1/bookings/my (Authorization: Bearer <JWT>)
    GW->>GW: JwtAuthenticationFilter validates 512-bit HS512 signature
    GW->>GW: Extract Claims: id, email, roles
    GW->>GW: Mutate downstream request headers:<br/>X-User-Id, X-User-Email, X-User-Roles
    GW->>Service: Forward request with identity headers
    Service-->>GW: HTTP 200 Response
    GW-->>UI: Return Data to UI
```

---

### Workflow 2: Hotel Discovery & Redis Cache-Aside Flow
```mermaid
sequenceDiagram
    autonumber
    actor User as Guest
    participant UI as React UI (:5173)
    participant GW as API Gateway (:8080)
    participant Hotel as Hotel Service (:8083)
    participant Redis as Redis Cache (:6379)
    participant DB as hms_hotel_db

    User->>UI: Browse Hotels / Filter by City
    UI->>GW: GET /api/v1/hotels?city=Goa
    GW->>Hotel: Forward request
    Hotel->>Redis: Check cache key: 'hotels::city:Goa'
    alt Cache HIT
        Redis-->>Hotel: Return cached Hotel list (JSON)
        Note over Hotel: Latency: ~2-5ms (Bypasses SQL)
    else Cache MISS
        Redis-->>Hotel: Cache Null
        Hotel->>DB: Execute JPA Specification query
        DB-->>Hotel: Return Hotel rows
        Hotel->>Redis: Populate cache with TTL (1 hour)
    end
    Hotel-->>GW: HTTP 200 List of Hotels
    GW-->>UI: Render Hotel cards on UI

    Note over Hotel,Redis: Cache Invalidation on Admin Update
    User->>GW: PUT /api/v1/hotels/1 (Admin role)
    GW->>Hotel: Forward update
    Hotel->>DB: Update Hotel row
    Hotel->>Redis: @CacheEvict(allEntries = true) flushes stale keys
    Hotel-->>GW: HTTP 200 Updated
```

---

### Workflow 3: Room Booking & Concurrency Control Flow
```mermaid
sequenceDiagram
    autonumber
    actor Guest as Guest
    participant UI as React UI (:5173)
    participant GW as API Gateway (:8080)
    participant Book as Booking Service (:8085)
    participant Room as Room Service (:8084)
    participant DB_Book as hms_booking_db
    participant Sched as BookingExpiryScheduler

    Guest->>UI: Select Room, Check-in: Oct 10, Check-out: Oct 15
    UI->>GW: POST /api/v1/bookings { roomId, checkIn, checkOut }
    GW->>Book: Forward request with X-User-Id header
    Book->>Book: Validate: checkOut > checkIn (min 1 night)
    
    Note over Book,Room: Step 1: Validate Room Status via Feign Delegate
    Book->>Room: GET /api/v1/rooms/{id} (RoomServiceClientDelegate)
    Room-->>Book: Return RoomDetailResponse (basePrice, capacity)

    Note over Book,DB_Book: Step 2: Mathematical Overlap Verification
    Book->>DB_Book: SELECT * FROM bookings WHERE room_id = :id<br/>AND status != 'CANCELLED'<br/>AND check_in < :newCheckOut AND check_out > :newCheckIn
    alt Conflict Found
        DB_Book-->>Book: Existing Conflicting Booking
        Book-->>GW: HTTP 409 Conflict ("Room already reserved for selected dates")
        GW-->>UI: Show Conflict Alert to Guest
    else No Conflict (Dates Available)
        DB_Book-->>Book: Empty result
        Book->>Room: PUT /api/v1/rooms/{id}/status { newStatus: 'BOOKED' }
        Room-->>Book: Status Updated
        Book->>DB_Book: INSERT Booking { status: 'PENDING_PAYMENT',<br/>holdExpiresAt: now() + 15 mins }
        DB_Book-->>Book: Booking Entity (ID: 101, Ref: HMS-BK-A1B2C3D4)
        Book-->>GW: HTTP 201 Created
        GW-->>UI: Open Payment Modal with 15-minute countdown timer
    end

    Note over Sched,Book: Background Sweeper (Runs every 60s)
    Sched->>DB_Book: SELECT * FROM bookings WHERE status = 'PENDING_PAYMENT'<br/>AND hold_expires_at < now()
    loop For each expired booking
        Sched->>DB_Book: UPDATE status = 'CANCELLED'
        Sched->>Room: PUT /api/v1/rooms/{id}/status { newStatus: 'AVAILABLE' }
    end
```

---

### Workflow 4: Billing, Tax Invoicing & Payment Simulation Flow
```mermaid
sequenceDiagram
    autonumber
    actor Guest as Guest
    participant UI as React UI (:5173)
    participant GW as API Gateway (:8080)
    participant Bill as Billing Service (:8088)
    participant Book as Booking Service (:8085)
    participant DB_Bill as hms_billing_db
    participant Kafka as Event Broker / Notification Service

    Guest->>UI: Enter Card Number, Expiry, CVV (Promo: WELCOME10)
    UI->>GW: POST /api/v1/billing/pay { bookingId, cardNumber, discountCode }
    GW->>Bill: Forward payment request
    
    Bill->>Book: GET /api/v1/bookings/{id} (via BookingServiceClientDelegate)
    Book-->>Bill: Return Booking Details (Base Amount: ₹15,000)

    Bill->>Bill: Calculate 10% Discount: ₹1,500
    Bill->>Bill: Calculate 18% GST (CGST 9% = ₹1,350, SGST 9% = ₹1,350)
    Bill->>Bill: Grand Total = ₹15,000 + ₹2,700 - ₹1,500 = ₹16,200

    alt Card ends in '9999' (Simulated Failure Trigger)
        Bill->>DB_Bill: INSERT Bill { status: 'FAILED', response: 'DECLINED' }
        Bill-->>GW: HTTP 402 / 400 ("Payment declined by simulated gateway")
        GW-->>UI: Display Card Decline Alert
    else Valid Card (Success Scenario)
        Bill->>DB_Bill: INSERT Bill { status: 'PAID', invoiceNumber: 'INV-2026-X', authCode: 'AUTH-123456' }
        DB_Bill-->>Bill: Saved Bill Entity
        
        Note over Bill,Book: Resilient Inter-Service Confirmation
        Bill->>Book: POST /api/v1/bookings/{id}/confirm-payment
        Book-->>Bill: Booking confirmed (status -> 'CONFIRMED')
        
        Bill->>Kafka: Publish PaymentProcessedEvent (BillId, InvoiceNo, CustomerEmail)
        Bill-->>GW: HTTP 200 BillResponse
        GW-->>UI: Payment Success! Show Confirmation & Tax Invoice Modal
    end
```

---

### Workflow 5: In-Room Dining & Kitchen Order Ticket (KOT) Flow
```mermaid
sequenceDiagram
    autonumber
    actor Guest as In-Room Guest
    actor Chef as Kitchen Staff
    participant UI as React UI (:5173)
    participant GW as API Gateway (:8080)
    participant RSM as Room Service Management (:8087)
    participant Food as Food Service (:8086)
    participant DB_RSM as hms_rsm_db

    Guest->>UI: Browse Dining Menu, Add items to Cart
    UI->>GW: POST /api/v1/room-service/orders { bookingId, roomNumber, items: [1, 2] }
    GW->>RSM: Forward dining order
    
    Note over RSM,Food: Batch Item Lookup (Eliminates N+1 Feign Calls)
    RSM->>Food: POST /api/v1/food/batch { itemIds: [1, 2] } (FoodServiceClientDelegate)
    Food-->>RSM: Return Food Item Catalog details (Names, Current Prices, Prep Times)

    RSM->>RSM: Snapshot Prices into Line Items (Financial Immutability)
    RSM->>RSM: Calculate: Subtotal + 5% Dining GST + ₹30 Delivery Fee
    RSM->>RSM: Estimate Delivery Time = max(prepTimes) + 15 min buffer
    RSM->>RSM: Generate Sequential KOT: 'KOT-70565'
    RSM->>DB_RSM: INSERT Order { status: 'ORDERED', kotNumber: 'KOT-70565' }
    RSM-->>GW: HTTP 201 Created OrderResponse
    GW-->>UI: Order Placed! Display Live KOT Tracker on Guest Portal

    Note over Chef,RSM: Kitchen Kanban Progression
    Chef->>UI: View Admin Operations -> Kitchen Kanban Board
    Chef->>GW: PATCH /api/v1/room-service/orders/{id}/status { status: 'PREPARING' }
    GW->>RSM: Update Order status to PREPARING
    Note over RSM: Once in PREPARING, cancellations are strictly rejected!
    Chef->>GW: PATCH /api/v1/room-service/orders/{id}/status { status: 'OUT_FOR_DELIVERY' }
    Chef->>GW: PATCH /api/v1/room-service/orders/{id}/status { status: 'DELIVERED' }
    RSM->>DB_RSM: Set delivered_at = now()
    RSM-->>UI: Guest UI updates to DELIVERED in real time
```

---

### Workflow 6: Hotel Inventory & Stock Replenishment Flow
```mermaid
sequenceDiagram
    autonumber
    actor Staff as Housekeeping Staff
    actor Admin as Property Manager
    participant UI as React UI (:5173)
    participant GW as API Gateway (:8080)
    participant Inv as Inventory Service (:8089)
    participant DB_Inv as hms_inventory_db

    Staff->>UI: Housekeeping requests 5 Deluxe Bed Linen Sets
    UI->>GW: POST /api/v1/inventory/{id}/consume { quantity: 5, department: 'HOUSEKEEPING' }
    GW->>Inv: Forward stock consumption request
    
    Inv->>DB_Inv: SELECT * FROM inventory_items WHERE id = :id FOR UPDATE
    DB_Inv-->>Inv: Item (available: 12, reorderLevel: 10, version: 1)
    
    Inv->>Inv: Verify: available >= 5 (12 >= 5 -> OK)
    Inv->>DB_Inv: UPDATE inventory_items SET quantity_available = 7, version = 2
    Inv->>DB_Inv: INSERT stock_movement_logs { type: 'CONSUMPTION', qty: -5, balance: 7 }

    alt Current Quantity <= Reorder Level (7 <= 10)
        Inv->>Inv: Flag lowStockAlert = true
        Note over Inv,UI: Automated Low-Stock Alert triggered!
    end

    Inv-->>GW: HTTP 200 Updated InventoryResponse
    GW-->>UI: Housekeeping confirms items checked out

    Admin->>UI: Admin opens Inventory Dashboard -> sees Red Alert on Linen
    Admin->>UI: Clicks "+ Restock (50 units)"
    UI->>GW: POST /api/v1/inventory/{id}/restock { quantity: 50, supplier: 'Luxury Fabrics Ltd' }
    GW->>Inv: Forward restock request
    Inv->>DB_Inv: UPDATE quantity_available = 57, version = 3
    Inv->>DB_Inv: INSERT stock_movement_logs { type: 'RESTOCK', qty: +50, balance: 57 }
    Inv-->>UI: Inventory Restocked, alert cleared
```

---

## 5. Resilience & Fault Tolerance Architecture

Every synchronous inter-service OpenFeign communication path is wrapped by a **Resilience4j Circuit Breaker** and **Retry with Exponential Backoff** implemented via dedicated `@Component` client delegates.

```mermaid
stateDiagram-v2
    [*] --> CLOSED : Service Normal

    state CLOSED {
        [*] --> Monitoring
        Monitoring --> Evaluation : Requests executed via Feign
        Evaluation --> Monitoring : Failure Rate < 50%
    }

    CLOSED --> OPEN : Failure Rate >= 50% across 5+ calls
    note right of OPEN
        All calls FAST-FAIL immediately!
        Executes typed fallback method.
        Prevents thread pool starvation.
    end note

    OPEN --> HALF_OPEN : After 10s waitDurationInOpenState
    note right of HALF_OPEN
        Sends 3 probe requests.
        Verifies downstream recovery.
    end note

    HALF_OPEN --> CLOSED : All 3 probe calls SUCCEED
    HALF_OPEN --> OPEN : Any probe call FAILS
```

---

## 6. Infrastructure Port Map

| Component | Port | Technology | Protocol / Format |
| :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | Spring Cloud Gateway (Netty) | HTTP / REST |
| **Auth Service** | `8081` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Customer Service** | `8082` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Hotel Service** | `8083` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Room Service** | `8084` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Booking Service** | `8085` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Food Service** | `8086` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Room Service Mgmt** | `8087` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Billing Service** | `8088` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Inventory Service** | `8089` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **Notification Service** | `8090` | Spring Boot 3.3.4 (Tomcat) | HTTP / REST |
| **React UI** | `5173` | React 18 + Vite | HTTP / HTML / JS |
| **PostgreSQL Database** | `5432` | PostgreSQL 16 Alpine | JDBC (10 isolated schemas) |
| **Redis Cache** | `6379` | Redis 7 Alpine | RESP (Cache-Aside) |
