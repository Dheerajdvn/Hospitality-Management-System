# MASTER PROJECT INTERVIEW DOCUMENT
## Grand Luxe Hospitality Management System — Java Spring Boot Microservices
**Target Role:** Java Backend Developer (3–4+ Years Experience)  
**Document Purpose:** Single-source master interview preparation guide covering actual architecture, code implementations, configurations, trade-offs, and spoken interview scripts.

---

# 1. Project Overview

### 2–3 Sentence Interview Introduction (Spoken Script)
> *"I developed the backend for the **Grand Luxe Hospitality Management System**, an enterprise multi-property hotel reservation and operations platform built with **Java 21, Spring Boot 3.3.4, and Spring Cloud 2023.0.3**. The system uses a **Database-per-Service architecture with 10 dedicated PostgreSQL databases**, an event-driven messaging backbone powered by **Apache Kafka in KRaft mode**, and **Spring Cloud Netflix Eureka** with an edge **API Gateway** for dynamic service discovery and perimeter JWT security. My work focused on core microservice development, inter-service resilience using **Resilience4j**, concurrency control using **optimistic locking**, and real-time operational workflows."*

### Detailed Explanation
The **Grand Luxe Hospitality Management System (HMS)** is a full-lifecycle hospitality management platform designed to manage the end-to-end guest journey across multiple luxury hotel properties. In traditional hospitality systems, legacy monolithic architectures often suffer from tight schema coupling, single points of failure, and scalability bottlenecks during peak holiday booking seasons or rush dining hours.

This platform solves those challenges by decomposing core business domains into autonomous, decoupled microservices. Each domain manages its own data persistence, business invariants, and scalability profile.

#### What Problem It Solves:
1. **Double-Booking & Reservation Contention:** Prevents concurrent guests from booking the same room for overlapping dates using mathematical interval conflict queries and Hibernate `@Version` optimistic locking.
2. **Cascading Failures Across Operational Units:** Ensures that if in-room dining, inventory, or notification services experience downtime or high load, core hotel browsing and room booking continue without interruption.
3. **Data Coupling & Schema Locks:** Eliminates cross-table SQL joins between reservations, menus, and payments by strictly enforcing the Database-per-Service pattern across 10 isolated PostgreSQL databases.
4. **Kitchen Order Latency:** Automates Kitchen Order Ticket (KOT) lifecycles and batch recipe resolution, eliminating the N+1 network call anti-pattern between dining and menu services.
5. **Decoupled Notification Delivery:** Shields critical reservation flows from third-party email/SMS provider latency by publishing domain events to Apache Kafka.

#### Main Users and Actors:
- **Guests / Customers:** Search hotel properties, filter available rooms, place room bookings, order in-room dining, view invoices, and complete digital payments.
- **Front Desk Staff & Hotel Managers:** Check guests in and out, assign rooms, update room housekeeping statuses (`CLEAN`, `DIRTY`, `MAINTENANCE`), and monitor reservation queues.
- **Kitchen & Dining Staff:** Receive Kitchen Order Tickets (KOT) in real-time, transition orders (`RECEIVED` $\to$ `PREPARING` $\to$ `READY` $\to$ `DELIVERED`), and track food prep times.
- **Inventory & Store Managers:** Manage hotel consumable stock levels, track automated batch deductions upon order delivery, receive low-stock reorder alerts, and log supplier restocks.
- **System Administrators:** Manage user accounts, enforce Role-Based Access Control (RBAC), configure hotel property metadata, and manage system-wide pricing rules.

---

# 2. Project in Simple Words

If an interviewer asks: *"What does your project actually do?"*, here is how to explain it in everyday conversational English:

> *"Think of this system as the digital brain behind a luxury hotel chain like Marriott or Taj.
> 
> When a guest opens our web portal, they can browse luxury hotels across different cities. Because hotel details rarely change every minute, we cache hotel profiles in **Redis** so the page loads in less than 2 milliseconds without hitting our database.
> 
> When the guest picks dates and books a room, our **Booking Service** checks that no one else has reserved that room for those overlapping dates, and places a **15-minute temporary hold** on the room. 
> 
> Next, the guest goes to payment. Our **Billing Service** calculates taxes (18% GST in our project) and simulates the payment gateway. Once the payment succeeds, an event is sent to **Apache Kafka**.
> 
> The **Booking Service** listens to this payment event and immediately confirms the reservation. At the same time, our **Notification Service** also picks up the event from Kafka and sends a confirmation email and SMS to the guest with their reservation voucher.
> 
> Once the guest arrives at the hotel, they can order food from room service using their phone. Our **Room Service Management** service validates that the guest is actively checked into that room, retrieves the menu items in a single batch call from **Food Service**, and sends the order to the kitchen. When the chef finishes cooking and the food is delivered, another Kafka event fires to our **Inventory Service**, which automatically deducts the cooking ingredients—like rice, milk, or coffee—from the warehouse inventory ledger.
> 
> Everything sits behind a central **API Gateway** that checks the user's login token before letting any request reach the internal services, and all services find each other automatically using **Eureka Service Discovery**."*

---

# 3. Business Flow

Below are the four primary end-to-end business flows that exist in the actual codebase:

### Flow 1: Hotel Room Booking & Confirmation
```text
Guest Browser 
  → API Gateway (:8080) [Validates JWT & Injects X-User-Id]
  → Booking Service (:8085) [POST /api/v1/bookings]
  → Room Client (Feign) → Room Service (:8084) [Validates Room Exists & Status == AVAILABLE]
  → Customer Client (Feign) → Customer Service (:8082) [Validates Customer Active]
  → Booking DB (PostgreSQL hms_booking_db) [Checks Date Overlap & Saves status = PENDING_PAYMENT]
  → Booking Service emits Kafka event to 'hms.booking.events' (BOOKING_CREATED)
  → Notification Service (:8090) consumes event & logs SMS/Email notification
  → Returns Booking ID & Payment Due details to Guest
```

### Flow 2: Invoice Generation & Payment Reconciliation
```text
Guest Browser 
  → API Gateway (:8080) 
  → Billing Service (:8088) [POST /api/v1/billing/invoices/generate]
  → Booking Client (Feign) → Booking Service (:8085) [Fetches booking details & room rate]
  → Billing Service computes base amount + 18% GST tax
  → Saves Invoice in hms_billing_db
  → Guest calls [POST /api/v1/billing/payments/process]
  → Billing Service processes payment (Credit Card / UPI / Net Banking)
  → Updates Invoice status to PAID
  → Billing Service publishes Kafka event to 'hms.payment.events' (PAYMENT_COMPLETED)
  → Booking Service (:8085) PaymentEventConsumer receives event & marks booking CONFIRMED
  → Notification Service (:8090) BookingEventConsumer receives event & sends receipt
```

### Flow 3: In-Room Dining (KOT) & Automated Inventory Deduction
```text
Guest in Room 
  → API Gateway (:8080)
  → Room Service Management (:8087) [POST /api/v1/room-service/orders]
  → BookingServiceClient (Feign) → Booking Service (:8085) [Verifies active booking for room]
  → FoodServiceClient (Feign) → Food Service (:8086) [POST /api/v1/food/batch to fetch dishes & prices]
  → Saves Order in hms_rsm_db with frozen price snapshots (status = PLACED)
  → Kitchen Staff transitions order: PLACED → PREPARING → READY → DELIVERED
  → Upon DELIVERED status, RSM publishes event to 'hms.roomservice.events'
  → Inventory Service (:8089) RoomServiceOrderEventConsumer receives event
  → Inventory Service deducts recipe ingredients and writes double-entry StockMovementLog in hms_inventory_db
  → If stock falls below reorderThreshold, Inventory Service publishes event to 'hms.inventory.events'
  → Notification Service receives alert and warns hotel store manager
```

### Flow 4: 15-Minute Unpaid Hold Expiration Sweep
```text
Spring Boot Scheduled Worker (Booking Service :8085)
  → Fires every 60 seconds (@Scheduled(cron = "0 */1 * * * *"))
  → Executes SQL: SELECT * FROM bookings WHERE status = 'PENDING_PAYMENT' AND created_at < NOW() - 15 minutes
  → For each expired record:
      - Sets status = CANCELLED
      - Publishes CANCELLED event to 'hms.booking.events'
  → Notification Service consumes event & notifies customer that the hold expired
  → Room is now immediately unlocked and available for other guests to reserve
```

---

# 4. Technology Stack

The following technologies were verified directly from the project's root and module `pom.xml` files, configuration files, and source code:

| Technology | Version in Project | Where Used | Why It Is Used |
|---|---|---|---|
| **Java** | `21` (LTS) | All 12 microservice modules | Uses modern LTS features: Virtual Threads, Records for immutable DTOs, enhanced pattern matching, and garbage collection improvements. |
| **Spring Boot** | `3.3.4` | All microservice modules | Core enterprise framework providing autoconfiguration, dependency injection, and embedded Netty/Tomcat runtime. |
| **Spring Cloud** | `2023.0.3` (Leyton) | Gateway, Eureka, OpenFeign, LoadBalancer | Cloud-native microservice support across the entire fleet. |
| **Spring WebFlux / Netty** | Bundled in Spring Cloud Gateway | `api-gateway` (:8080) | Non-blocking, event-driven reactive perimeter proxy handling high concurrent I/O with low thread overhead. |
| **Spring MVC / Tomcat** | Bundled in Spring Boot Starter Web | All 10 domain microservices (:8081–:8090) | Standard servlet-based web layer for synchronous RESTful API endpoints. |
| **Spring Cloud Netflix Eureka** | `spring-cloud-starter-netflix-eureka-server` / `client` | `eureka-server` (:8761) & all 11 client services | Centralized dynamic service registry and health discovery. Replaces hardcoded IPs. |
| **Spring Cloud OpenFeign** | `spring-cloud-starter-openfeign` (with `feign-hc5`) | `booking-service`, `billing-service`, `room-service`, `food-service`, `room-service-management`, `inventory-service` | Declarative REST client using Apache HttpClient 5 connection pooling for inter-service HTTP calls. |
| **Resilience4j** | `2.2.0` (`resilience4j-spring-boot3`) | `booking-service`, `billing-service`, `room-service`, `room-service-management`, `inventory-service` | Circuit breaker, retry with exponential backoff, and fallback handling for inter-service Feign calls. |
| **Spring Data JPA & Hibernate** | `spring-boot-starter-data-jpa` | All 10 domain services (:8081–:8090) | Relational object-relational mapping, repositories, and transaction management. |
| **PostgreSQL** | `16` (via Docker Desktop container `hms-postgres`) | Dedicated database per service | Enterprise ACID relational storage. 10 independent databases enforce schema isolation. |
| **Redis** | `7` (via Docker Desktop container `hms-redis`) | `hotel-service` (:8083) | In-memory Cache-Aside store for high-frequency hotel catalog read queries (`hotel_cache`). |
| **Apache Kafka** | `3.7.0` (KRaft mode, container `hms-kafka`) | `booking-service`, `billing-service`, `room-service-management`, `inventory-service`, `notification-service` | High-throughput, asynchronous, partitioned distributed event stream. KRaft replaces ZooKeeper. |
| **Spring Security** | `spring-boot-starter-security` | `auth-service` (:8081) | BCrypt password hashing (`strength = 12`) and security filter chain for authentication. |
| **JJWT (Java JWT)** | `0.12.5` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | `auth-service` (:8081) and `api-gateway` (:8080) | Generates and verifies cryptographic 512-bit HS512 signed JSON Web Tokens. |
| **Jakarta Validation** | `spring-boot-starter-validation` | All domain services | Declarative DTO input validation (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Email`). |
| **Spring Boot Actuator** | `spring-boot-starter-actuator` | All microservices | Operational telemetry, health checks (`/actuator/health`), circuit breaker states, and metrics. |
| **Project Lombok** | `1.18.34` | All microservices | Reduces boilerplate code (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`). |
| **JUnit 5 & Mockito** | `junit-jupiter`, `mockito-core`, `mockito-junit-jupiter` | All modules (`src/test/java`) | Comprehensive unit and integration testing (102 passing automated tests in project). |
| **Docker & Docker Compose** | Docker Engine & Compose file `infrastructure/docker/docker-compose-infra.yml` | Infrastructure layer | Orchestrates local infrastructure (PostgreSQL 16, Redis 7, Kafka KRaft 3.7.0) with health checks. |
| **React & Vite** | React `19.2.8`, Vite `8.3.0`, Axios `1.20.0` | `frontend/hospitality-ui` (:5173) | Single Page Application (SPA) frontend portal consuming Gateway APIs. |
| **Git** | `git` | Root repository | Version control system tracking all microservice source code and architecture documentation. |

### Technologies Explicitly Checked and NOT Present in Project:
- **Kubernetes (K8s):** *Not found / Not confirmed from the project.* (Infrastructure is managed via Docker Compose).
- **AWS / Cloud Infrastructure (EC2, S3, RDS, EKS):** *Not found / Not confirmed from the project.*
- **CI/CD Pipelines (Jenkins, GitHub Actions, GitLab CI):** *Not found / Not confirmed from the project.*
- **Distributed Tracing (Zipkin / Jaeger / Spring Cloud Sleuth):** *Not found / Not confirmed from the project.*
- **Centralized Logging (ELK Stack / Logstash / Splunk):** *Not found / Not confirmed from the project.* (Logs output via SLF4J/Logback to console).
- **ZooKeeper:** *Not found / Not used.* (Apache Kafka runs natively in KRaft consensus mode).
- **Spring Cloud Config Server:** *Not found / Not used.* (Configuration is kept in local `application.yml` per service).

---

# 5. Microservices List

The system consists of **12 backend Java microservices + 1 React frontend application**:

```text
1. eureka-server               (Port: 8761)
2. api-gateway                 (Port: 8080)
3. auth-service                (Port: 8081)
4. customer-service            (Port: 8082)
5. hotel-service               (Port: 8083)
6. room-service                (Port: 8084)
7. booking-service             (Port: 8085)
8. food-service                (Port: 8086)
9. room-service-management     (Port: 8087)
10. billing-service            (Port: 8088)
11. inventory-service          (Port: 8089)
12. notification-service       (Port: 8090)
13. hospitality-ui             (Port: 5173 - React/Vite Frontend Portal)
```

---

# 6. Responsibility of Each Microservice

---

## Service 1: `eureka-server`
- **Port:** `8761`
- **Purpose:** Centralized service registry and discovery server. Tracks network locations (IP, port, health state) of all active microservices.
- **Main APIs / Endpoints:**
  - `GET /` — Eureka web dashboard showing registered instances and renewal rates.
  - `GET /eureka/apps` — REST endpoint returning registered application clusters.
- **Database:** None (In-memory concurrent hash maps for registration records).
- **Communication:** Passive server. Receives registrations and 30-second renewal heartbeats from all 11 client services.
- **Kafka:** Not used.
- **Eureka:** It is the Eureka Server (`@EnableEurekaServer`). Sets `register-with-eureka: false` and `fetch-registry: false`.
- **Important Classes:** `EurekaServerApplication`.
- **Interview Explanation:** *"Eureka Server is our service registry running on port 8761. When any microservice boots up, it registers its IP and port with Eureka. The API Gateway and Feign clients query Eureka to resolve virtual service names into live network endpoints, enabling seamless load balancing."*

---

## Service 2: `api-gateway`
- **Port:** `8080`
- **Purpose:** Single entry point (reverse proxy) for all frontend and external traffic. Handles routing, dynamic load balancing (`lb://`), CORS preflight, and perimeter JWT authentication.
- **Main APIs:** Routes all `/api/v1/**` paths to downstream services.
  - Actuator endpoints: `/actuator/health`, `/actuator/gateway/routes`.
- **Database:** None.
- **Communication:** Non-blocking Netty WebFlux. Forwards requests to downstream services using Spring Cloud LoadBalancer.
- **Security:** `JwtAuthenticationFilter` (`GlobalFilter` with order `-100`). Validates Bearer token signature with JJWT, enforces route-level RBAC (e.g. `ROLE_ADMIN` for hotel creation), strips spoofed headers, and injects `X-User-Id`, `X-User-Email`, `X-User-Roles` into the downstream request.
- **Kafka:** Not used.
- **Eureka:** Registered Eureka Client. Resolves URIs like `lb://booking-service`.
- **Important Classes:** `ApiGatewayApplication`, `JwtAuthenticationFilter`, `JwtUtils`.
- **Interview Explanation:** *"Our API Gateway runs on Spring Cloud Gateway with Netty. It acts as our perimeter security guard. It intercepts every request, validates the JWT, checks if the user has the required role, strips any spoofed user headers, and forwards the request with validated user identity headers to downstream services."*

---

## Service 3: `auth-service`
- **Port:** `8081`
- **Purpose:** Manages user authentication, registration, password hashing, role assignment, and JWT issuance.
- **Main APIs:**
  - `POST /api/v1/auth/login` — Authenticates credentials; returns 512-bit HS512 JWT.
  - `POST /api/v1/auth/register` — Registers new users with BCrypt-hashed passwords.
  - `GET /api/v1/auth/me` — Returns currently authenticated user details.
  - `POST /api/v1/auth/validate` — Validates token validity.
- **Database:** PostgreSQL (`hms_auth_db`).
- **Entities:** `User`, `Role`, `RoleName` (`ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_CUSTOMER`).
- **Repositories:** `UserRepository`, `RoleRepository`.
- **Communication:** Receives calls forwarded from API Gateway.
- **Kafka:** Not used.
- **Important Classes:** `AuthController`, `AuthService`, `AuthServiceImpl`, `SecurityConfig`, `JwtTokenProvider`, `CustomUserDetailsService`.
- **Interview Explanation:** *"Auth Service is our identity provider on port 8081. It stores user credentials in `hms_auth_db` with BCrypt password hashing. Upon successful login, it signs a 512-bit HS512 JWT containing user claims and roles."*

---

## Service 4: `customer-service`
- **Port:** `8082`
- **Purpose:** Manages customer profiles, contact numbers, addresses, and Know-Your-Customer (KYC) identity details.
- **Main APIs:**
  - `GET /api/v1/customers/{id}` — Fetches customer profile.
  - `POST /api/v1/customers` — Creates a new guest profile.
  - `PUT /api/v1/customers/{id}` — Updates profile details.
  - `GET /api/v1/customers/{id}/kyc-status` — Validates guest KYC status.
- **Database:** PostgreSQL (`hms_customer_db`).
- **Entities:** `CustomerProfile`, `Address`.
- **Repositories:** `CustomerProfileRepository`.
- **Communication:** Called by `booking-service` via OpenFeign to verify guest account status before accepting reservations.
- **Kafka:** Not used.
- **Important Classes:** `CustomerController`, `CustomerService`, `CustomerServiceImpl`, `CustomerProfileRepository`.
- **Interview Explanation:** *"Customer Service manages guest master records on port 8082. When a booking is placed, Booking Service calls Customer Service over Feign to ensure the guest profile exists and is active."*

---

## Service 5: `hotel-service`
- **Port:** `8083`
- **Purpose:** Manages hotel property catalogs, cities, star ratings, amenities, and search filters.
- **Main APIs:**
  - `GET /api/v1/hotels` — Search hotels by city, star rating, or keyword.
  - `GET /api/v1/hotels/{id}` — Fetches hotel profile (Cached via Redis).
  - `POST /api/v1/hotels` — Creates new hotel property (`ROLE_ADMIN` only).
  - `PUT /api/v1/hotels/{id}` — Updates hotel details (Evicts Redis cache).
- **Database:** PostgreSQL (`hms_hotel_db`) + Redis 7 cache (`hotel_cache`).
- **Entities:** `Hotel`.
- **Repositories:** `HotelRepository` (uses `JpaSpecificationExecutor` for dynamic search filters).
- **Communication:** Called by `room-service` and `food-service` via OpenFeign to validate that the hotel exists.
- **Caching Logic:** Uses Spring Data Redis `@Cacheable(value = "hotel_cache", key = "#id")` and `@CacheEvict`.
- **Kafka:** Not used.
- **Important Classes:** `HotelController`, `HotelService`, `HotelServiceImpl`, `HotelRepository`, `RedisConfig`.
- **Interview Explanation:** *"Hotel Service runs on port 8083 and handles property catalogs. Because hotel descriptions and amenities are read frequently but updated rarely, we implemented the Cache-Aside pattern using Redis. Reads return in under 2ms, and updates evict the cache immediately."*

---

## Service 6: `room-service`
- **Port:** `8084`
- **Purpose:** Manages physical rooms, room numbers, room categories (`DELUXE`, `SUITE`, `STANDARD`), base prices, and housekeeping states (`AVAILABLE`, `OCCUPIED`, `MAINTENANCE`, `DIRTY`).
- **Main APIs:**
  - `GET /api/v1/rooms/hotel/{hotelId}` — Lists rooms for a hotel.
  - `GET /api/v1/rooms/{id}` — Gets room details by ID.
  - `PATCH /api/v1/rooms/{id}/status` — Updates housekeeping/operational status.
  - `POST /api/v1/rooms` — Adds a room to a hotel.
- **Database:** PostgreSQL (`hms_room_db`).
- **Entities:** `Room` (includes `@Version Long version` for optimistic locking), `RoomStatusLog`.
- **Repositories:** `RoomRepository`, `RoomStatusLogRepository`.
- **Communication:**
  - Calls `hotel-service` via OpenFeign (`HotelClient`) to ensure the hotel exists.
  - Called by `booking-service` via OpenFeign (`RoomClient`) to verify room availability and price.
- **Kafka:** Not used.
- **Important Classes:** `RoomController`, `RoomService`, `RoomServiceImpl`, `RoomRepository`, `HotelClient`.
- **Interview Explanation:** *"Room Service manages room inventory on port 8084. It tracks room pricing, amenities, and housekeeping states. To prevent concurrent update conflicts, the `Room` entity uses Hibernate `@Version` optimistic locking."*

---

## Service 7: `booking-service`
- **Port:** `8085`
- **Purpose:** Core reservation engine. Handles room booking creation, double-booking prevention, 15-minute hold lifecycles, and reservation status transitions.
- **Main APIs:**
  - `POST /api/v1/bookings` — Creates a reservation hold (`PENDING_PAYMENT`).
  - `GET /api/v1/bookings/{id}` — Gets reservation details.
  - `POST /api/v1/bookings/{id}/confirm` — Confirms booking after payment.
  - `POST /api/v1/bookings/{id}/cancel` — Cancels a booking.
- **Database:** PostgreSQL (`hms_booking_db`).
- **Entities:** `Booking`, `BookingStatus` (`PENDING_PAYMENT`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`).
- **Repositories:** `BookingRepository`.
- **Communication:**
  - Calls `customer-service` (`CustomerClient`) and `room-service` (`RoomClient`) via OpenFeign protected by Resilience4j delegates.
  - Publishes events to Kafka topic `hms.booking.events`.
  - Consumes payment events from Kafka topic `hms.payment.events` via `PaymentEventConsumer` to auto-confirm bookings.
- **Scheduled Jobs:** `@Scheduled(cron = "0 */1 * * * *")` sweeps unpaid reservations older than 15 minutes and cancels them.
- **Important Classes:** `BookingController`, `BookingService`, `BookingServiceImpl`, `BookingRepository`, `BookingServiceClientDelegate`, `KafkaBookingEventPublisher`, `PaymentEventConsumer`.
- **Interview Explanation:** *"Booking Service on port 8085 is our central reservation engine. It prevents double-bookings using a mathematical date overlap query. When a room is booked, it puts a 15-minute hold on it. When payment succeeds, it consumes an event from Kafka to mark the booking confirmed. It also runs a scheduled background job every minute to release expired holds."*

---

## Service 8: `food-service`
- **Port:** `8086`
- **Purpose:** Manages hotel dining menus, dish descriptions, categories (`APPETIZER`, `MAIN_COURSE`, `BEVERAGE`, `DESSERT`), dietary tags (`VEG`, `NON_VEG`, `VEGAN`), and current prices.
- **Main APIs:**
  - `GET /api/v1/food/hotel/{hotelId}` — Lists restaurant menu for a hotel.
  - `POST /api/v1/food/batch` — High-efficiency batch lookup: accepts `{ foodItemIds: [1, 2, 5] }` and returns dish names and prices in a single query.
  - `POST /api/v1/food` — Adds menu item.
- **Database:** PostgreSQL (`hms_food_db`).
- **Entities:** `FoodItem`.
- **Repositories:** `FoodItemRepository`.
- **Communication:**
  - Calls `hotel-service` via OpenFeign (`HotelClient`).
  - Called by `room-service-management` via OpenFeign (`FoodServiceClient`) using the `POST /batch` endpoint.
- **Kafka:** Not used.
- **Important Classes:** `FoodController`, `FoodService`, `FoodServiceImpl`, `FoodItemRepository`.
- **Interview Explanation:** *"Food Service runs on port 8086 and manages menu catalogs. A key design highlight is its `POST /batch` endpoint, which lets the Room Service Management service resolve multiple food items in a single HTTP call, avoiding the N+1 network call anti-pattern."*

---

## Service 9: `room-service-management`
- **Port:** `8087`
- **Purpose:** Manages in-room dining orders, Kitchen Order Tickets (KOT), prep time estimations, and delivery status transitions (`PLACED` $\to$ `PREPARING` $\to$ `READY` $\to$ `DELIVERED`).
- **Main APIs:**
  - `POST /api/v1/room-service/orders` — Places an in-room dining order.
  - `GET /api/v1/room-service/orders/{id}` — Gets order details and current KOT state.
  - `PATCH /api/v1/room-service/orders/{id}/status` — Transitions order status.
- **Database:** PostgreSQL (`hms_rsm_db`).
- **Entities:** `RoomServiceOrder`, `RoomServiceOrderItem` (stores frozen price snapshots `unitPrice` and `foodItemName`).
- **Repositories:** `RoomServiceOrderRepository`.
- **Communication:**
  - Calls `booking-service` (`BookingServiceClient`) to verify guest booking.
  - Calls `food-service` (`FoodServiceClient`) via `POST /batch` to fetch items and prices.
  - Publishes events to Kafka topic `hms.roomservice.events` when status becomes `DELIVERED`.
- **Important Classes:** `RoomServiceOrderController`, `RoomServiceOrderService`, `RoomServiceOrderServiceImpl`, `KafkaRoomServiceEventPublisher`.
- **Interview Explanation:** *"Room Service Management runs on port 8087. It handles room dining orders. When an order is placed, it takes a frozen price snapshot of each menu item into `room_service_order_items` so future menu price changes never alter past receipts. When an order is delivered, it emits a Kafka event so inventory can be deducted automatically."*

---

## Service 10: `billing-service`
- **Port:** `8088`
- **Purpose:** Generates invoices, applies 18% GST tax, processes payments (Credit Card, UPI, Net Banking), and handles payment reconciliation.
- **Main APIs:**
  - `POST /api/v1/billing/invoices/generate` — Creates invoice for a reservation.
  - `POST /api/v1/billing/payments/process` — Processes payment against an invoice.
  - `GET /api/v1/billing/invoices/{id}` — Retrieves invoice details.
- **Database:** PostgreSQL (`hms_billing_db`).
- **Entities:** `Bill` (stores invoice details, tax, payment status), `BillStatus`, `PaymentMethod`.
- **Repositories:** `BillRepository`.
- **Communication:**
  - Calls `booking-service` (`BookingClient`) via OpenFeign wrapped in `BookingServiceClientDelegate` with Resilience4j.
  - Publishes events to Kafka topics `hms.billing.events` and `hms.payment.events`.
- **Important Classes:** `BillingController`, `BillingService`, `BillingServiceImpl`, `BillRepository`, `BookingServiceClientDelegate`, `KafkaBillingEventPublisher`.
- **Interview Explanation:** *"Billing Service on port 8088 handles invoicing and payments. It fetches room rates from Booking Service, applies an 18% GST calculation, and records the invoice in `hms_billing_db`. Upon successful payment, it publishes a `PAYMENT_COMPLETED` event to Kafka so Booking Service can confirm the room."*

---

## Service 11: `inventory-service`
- **Port:** `8089`
- **Purpose:** Manages hotel consumables, kitchen raw materials, cleaning supplies, double-entry stock movement ledgers, and low-stock reorder thresholds.
- **Main APIs:**
  - `GET /api/v1/inventory/hotel/{hotelId}` — Lists inventory items for a property.
  - `POST /api/v1/inventory/items` — Registers a new inventory supply item.
  - `POST /api/v1/inventory/items/{id}/adjust` — Manual stock adjustment (restock, waste, audit).
  - `GET /api/v1/inventory/low-stock` — Lists items below reorder threshold.
- **Database:** PostgreSQL (`hms_inventory_db`).
- **Entities:** `InventoryItem` (`@Version` protected), `StockMovementLog` (audit ledger), `InventoryCategory`, `StockMovementType` (`PURCHASE`, `USAGE`, `WASTAGE`, `ADJUSTMENT`).
- **Repositories:** `InventoryItemRepository`, `StockMovementLogRepository`.
- **Communication:**
  - Calls `hotel-service` (`HotelClient`) via Feign.
  - Consumes Kafka events from `hms.roomservice.events` via `RoomServiceOrderEventConsumer` to automatically deduct kitchen stock upon order delivery.
  - Publishes Kafka events to `hms.inventory.events` when stock falls below threshold.
- **Important Classes:** `InventoryController`, `InventoryService`, `InventoryServiceImpl`, `RoomServiceOrderEventConsumer`, `KafkaInventoryEventPublisher`.
- **Interview Explanation:** *"Inventory Service on port 8089 tracks supplies like linens, toiletries, and food ingredients. It implements a double-entry stock ledger where every addition or deduction is recorded in `stock_movement_logs`. It listens to Kafka room service delivery events and automatically deducts stock without human intervention."*

---

## Service 12: `notification-service`
- **Port:** `8090`
- **Purpose:** Decoupled multi-channel notification dispatcher. Consumes domain events from Kafka and dispatches simulated email and SMS messages while recording delivery logs.
- **Main APIs:**
  - `GET /api/v1/notifications/customer/{customerId}` — Views notification history for a guest.
  - `POST /api/v1/notifications/simulate-event` — Test endpoint for manual verification.
- **Database:** PostgreSQL (`hms_notification_db`).
- **Entities:** `NotificationLog`, `NotificationChannel` (`EMAIL`, `SMS`), `NotificationStatus` (`SENT`, `FAILED`).
- **Repositories:** `NotificationLogRepository`.
- **Communication:**
  - Asynchronous consumer listening to 4 Kafka topics: `hms.booking.events`, `hms.payment.events`, `hms.roomservice.events`, and `hms.inventory.events`.
- **Important Classes:** `NotificationController`, `NotificationService`, `NotificationServiceImpl`, `BookingEventConsumer`, `OperationsEventConsumer`.
- **Interview Explanation:** *"Notification Service on port 8090 is completely event-driven. It listens to Kafka topics for booking confirmations, payment receipts, and low stock warnings. Because it consumes events asynchronously, third-party email or SMS delivery delays never slow down the guest's checkout experience."*

---

# 7. Complete Architecture

### Mermaid Architecture Diagram

```mermaid
flowchart TB
    subgraph Clients["Frontend Layer"]
        UI["React 18 + Vite SPA (:5173)<br/>Axios HTTP Client"]
    end

    subgraph Perimeter["Edge Security & Perimeter Routing"]
        GW["Spring Cloud API Gateway (:8080)<br/>Netty / WebFlux / LoadBalancer<br/>JwtAuthenticationFilter (Order: -100)"]
    end

    subgraph Discovery["Service Discovery Cluster"]
        Eureka["Spring Cloud Netflix Eureka Server (:8761)<br/>Service Registry & Dynamic Heartbeat"]
    end

    subgraph CoreServices["Domain Microservices Fleet (Spring Boot 3.3.4 / Java 21)"]
        Auth["auth-service (:8081)<br/>JJWT 512-bit / BCrypt"]
        Customer["customer-service (:8082)<br/>Guest Profiles & KYC"]
        Hotel["hotel-service (:8083)<br/>Catalog & Dynamic Search"]
        Room["room-service (:8084)<br/>Inventory & @Version Lock"]
        Booking["booking-service (:8085)<br/>Date Overlap & 15m Sweep"]
        Food["food-service (:8086)<br/>Menus & Batch Lookup"]
        RSM["room-service-mgmt (:8087)<br/>In-Room Dining & KOT State"]
        Billing["billing-service (:8088)<br/>18% GST & Payment Gateway"]
        Inventory["inventory-service (:8089)<br/>Double-Entry Stock Ledger"]
        Notification["notification-service (:8090)<br/>Multi-Channel Dispatcher"]
    end

    subgraph EventBus["Event-Driven Message Backbone (Apache Kafka 3.7.0 KRaft)"]
        T_Booking["hms.booking.events (3 Partitions)"]
        T_Payment["hms.payment.events (3 Partitions)"]
        T_RSM["hms.roomservice.events (3 Partitions)"]
        T_Inv["hms.inventory.events (3 Partitions)"]
    end

    subgraph Persistence["Persistence & In-Memory Storage"]
        DB_Auth[("hms_auth_db")]
        DB_Customer[("hms_customer_db")]
        DB_Hotel[("hms_hotel_db")]
        DB_Room[("hms_room_db")]
        DB_Booking[("hms_booking_db")]
        DB_Food[("hms_food_db")]
        DB_RSM[("hms_rsm_db")]
        DB_Billing[("hms_billing_db")]
        DB_Inv[("hms_inventory_db")]
        DB_Notif[("hms_notification_db")]
        RedisCache[("Redis 7 Cache<br/>hotel_cache")]
    end

    %% Client Routing
    UI -->|REST / CORS| GW

    %% Eureka Registration
    GW -.->|Route Resolution lb://| Eureka
    Auth -.->|Register & Heartbeat| Eureka
    Customer -.->|Register & Heartbeat| Eureka
    Hotel -.->|Register & Heartbeat| Eureka
    Room -.->|Register & Heartbeat| Eureka
    Booking -.->|Register & Heartbeat| Eureka
    Food -.->|Register & Heartbeat| Eureka
    RSM -.->|Register & Heartbeat| Eureka
    Billing -.->|Register & Heartbeat| Eureka
    Inventory -.->|Register & Heartbeat| Eureka
    Notification -.->|Register & Heartbeat| Eureka

    %% Gateway Dynamic Forwarding
    GW ==>|lb://auth-service| Auth
    GW ==>|lb://customer-service| Customer
    GW ==>|lb://hotel-service| Hotel
    GW ==>|lb://room-service| Room
    GW ==>|lb://booking-service| Booking
    GW ==>|lb://food-service| Food
    GW ==>|lb://room-service-management| RSM
    GW ==>|lb://billing-service| Billing
    GW ==>|lb://inventory-service| Inventory
    GW ==>|lb://notification-service| Notification

    %% Synchronous Feign Calls
    Room -->|Feign: HotelClient| Hotel
    Food -->|Feign: HotelClient| Hotel
    Inventory -->|Feign: HotelClient| Hotel
    Booking -->|Feign + Resilience4j| Customer
    Booking -->|Feign + Resilience4j| Room
    RSM -->|Feign: BookingServiceClient| Booking
    RSM -->|Feign: POST /batch| Food
    Billing -->|Feign + Resilience4j| Booking

    %% Asynchronous Kafka Flow
    Booking -->|Publishes BOOKING_CREATED| T_Booking
    Billing -->|Publishes PAYMENT_COMPLETED| T_Payment
    RSM -->|Publishes ORDER_DELIVERED| T_RSM
    Inventory -->|Publishes LOW_STOCK_ALERT| T_Inv

    T_Payment -->|Consumer: Auto-Confirm| Booking
    T_RSM -->|Consumer: Auto-Deduct Stock| Inventory
    T_Booking -->|Consumer: Email/SMS| Notification
    T_Payment -->|Consumer: Receipt| Notification
    T_RSM -->|Consumer: Status Alert| Notification
    T_Inv -->|Consumer: Admin Warning| Notification

    %% Database Connections
    Auth --> DB_Auth
    Customer --> DB_Customer
    Hotel --> DB_Hotel
    Hotel --> RedisCache
    Room --> DB_Room
    Booking --> DB_Booking
    Food --> DB_Food
    RSM --> DB_RSM
    Billing --> DB_Billing
    Inventory --> DB_Inv
    Notification --> DB_Notif
```

### Explanation of the Diagram in Simple English
1. **The Entry Door:** The React frontend makes all calls through a single address: `http://localhost:8080` (Spring Cloud Gateway). The client never talks directly to individual services.
2. **The Traffic Controller:** The Gateway asks Eureka where `booking-service` or `hotel-service` is currently running, checks if the user's JWT token is valid, and forwards the request.
3. **Real-time Queries (OpenFeign):** When `booking-service` needs to verify if a room is available right now, it makes a direct, synchronous HTTP call to `room-service` using OpenFeign protected by a Resilience4j circuit breaker.
4. **Side-Effects (Kafka):** When payment succeeds or a room service meal is delivered, services publish events to Kafka. Other services consume these events asynchronously to update status, deduct stock, or send SMS/email confirmations.
5. **Private Databases:** Every microservice has its own private PostgreSQL database (`hms_*_db`). No service can run a SQL join across another service's database tables.

---

# 8. Project Structure

### Repository Layout
```text
hospitality-management-system/
├── backend/                              # Root Maven multi-module parent
│   ├── pom.xml                           # Root aggregator POM (Java 21, Spring Boot 3.3.4)
│   ├── eureka-server/                    # Discovery server (:8761)
│   ├── api-gateway/                      # Spring Cloud Gateway (:8080)
│   ├── auth-service/                     # Identity & JWT provider (:8081)
│   ├── customer-service/                 # Guest profiles & KYC (:8082)
│   ├── hotel-service/                    # Hotel catalog & Redis cache (:8083)
│   ├── room-service/                     # Room inventory & @Version lock (:8084)
│   ├── booking-service/                  # Reservations & hold engine (:8085)
│   ├── food-service/                     # Menu items & batch lookup (:8086)
│   ├── room-service-management/          # In-room dining & KOT (:8087)
│   ├── billing-service/                  # Invoices & 18% GST payment (:8088)
│   ├── inventory-service/                # Supplies & stock ledger (:8089)
│   └── notification-service/             # Multi-channel Kafka consumer (:8090)
├── frontend/
│   └── hospitality-ui/                   # React 18 + Vite portal (:5173)
├── infrastructure/
│   ├── docker/
│   │   └── docker-compose-infra.yml      # PostgreSQL 16, Redis 7, Kafka 3.7.0 KRaft
│   ├── postgres/
│   │   └── init-databases.sh             # Shell script initializing all 10 databases
│   └── redis/
│       └── redis.conf                    # Redis memory ceiling & eviction config
├── docs/                                 # Architecture diagrams, runbooks & guides
├── start-all.ps1                         # PowerShell orchestration script to boot all services
├── stop-all.ps1                          # PowerShell orchestration script to stop fleet cleanly
└── check-health.ps1                      # Automated health check script for all 12 services
```

### Internal Package Layout (Standardized across services)
Each domain microservice strictly follows standard layered architecture:
```text
com.hospitality.<service>/
├── controller/         # REST Controllers exposing HTTP endpoints (@RestController)
├── service/            # Business logic interfaces and implementations (*Service, *ServiceImpl)
├── repository/         # Spring Data JPA repositories (*Repository extends JpaRepository)
├── entity/             # JPA Entity classes mapped to PostgreSQL tables (@Entity, @Table)
├── dto/                # Data Transfer Objects, request/response models, Java records
├── client/             # Spring Cloud OpenFeign client interfaces (@FeignClient)
├── delegate/           # Resilience4j circuit breaker delegate wrappers (*ServiceClientDelegate)
├── publisher/          # Kafka event publishers (KafkaTemplate)
├── consumer/           # Kafka event listeners (@KafkaListener)
├── config/             # Spring configuration beans (Security, Redis, Feign, Web, Kafka)
└── exception/          # GlobalExceptionHandler (@RestControllerAdvice) and custom exceptions
```

---

# 9. Request Flow

### Detailed Journey: Placing a Hotel Room Booking (`POST /api/v1/bookings`)

Here is the exact journey of the most critical API request in the system:

```text
Step 1: Client Request
Guest clicks "Book Room" on React UI (:5173).
The browser issues HTTP POST to:
http://localhost:8080/api/v1/bookings
Headers:
  Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Body:
  { "customerId": 1, "roomId": 5, "checkInDate": "2026-10-01", "checkOutDate": "2026-10-05" }

Step 2: API Gateway Processing (:8080)
1. Netty event loop receives the TCP connection.
2. JwtAuthenticationFilter (GlobalFilter, Order -100) intercepts the request.
3. Filter checks if path is whitelisted (No, /api/v1/bookings requires auth).
4. Validates Bearer token using JJWT HS512 secret key.
5. Extracts claims: userId=1, email="guest@hotel.com", roles=["ROLE_CUSTOMER"].
6. Strips any incoming spoofed headers and injects validated headers:
   X-User-Id: 1
   X-User-Email: guest@hotel.com
   X-User-Roles: ROLE_CUSTOMER
7. Gateway matches route predicate: Path=/api/v1/bookings/** -> uri: lb://booking-service.
8. Spring Cloud LoadBalancer queries Eureka Client cache for "booking-service", selects instance at localhost:8085, and forwards the request.

Step 3: Booking Controller Layer (:8085)
1. BookingController receives POST /api/v1/bookings.
2. Jakarta @Valid annotation triggers bean validation on CreateBookingRequest (checks @NotNull dates, etc.).
3. Calls bookingService.createBooking(request).

Step 4: Business Logic & Inter-Service Validation
1. BookingServiceImpl calls CustomerServiceClientDelegate.validateCustomer(customerId).
   -> Feign issues GET http://customer-service/api/v1/customers/1.
   -> Customer Service returns 200 OK (Customer active).
2. BookingServiceImpl calls RoomServiceClientDelegate.getRoom(roomId).
   -> Feign issues GET http://room-service/api/v1/rooms/5.
   -> Room Service returns 200 OK (Room price = 4500.00, status = AVAILABLE).

Step 5: Mathematical Interval Conflict Check & Database Lock
1. BookingServiceImpl executes conflict query in BookingRepository:
   SELECT COUNT(b) FROM Booking b 
   WHERE b.room.id = 5 
     AND b.status IN ('CONFIRMED', 'PENDING_PAYMENT')
     AND b.checkInDate < '2026-10-05' 
     AND b.checkOutDate > '2026-10-01'
2. Query returns 0 (No active booking overlaps these dates).
3. BookingServiceImpl creates Booking entity:
   - status = PENDING_PAYMENT
   - totalAmount = 4 nights * 4500.00 = 18,000.00
   - holdExpiresAt = LocalDateTime.now().plusMinutes(15)
4. bookingRepository.save(booking) writes record into PostgreSQL hms_booking_db.

Step 6: Asynchronous Event Emission (Kafka)
1. KafkaBookingEventPublisher publishes BookingCreatedEvent to topic 'hms.booking.events' with key "booking-101".
2. Event payload contains bookingId, customerId, roomId, totalAmount, and timestamp.
3. Does not wait for Kafka consumers (non-blocking).

Step 7: Downstream Kafka Consumption
1. Notification Service's BookingEventConsumer receives the message.
2. Formats a reservation hold notification and writes record to hms_notification_db.notification_logs.

Step 8: Response to Client
1. BookingController returns HTTP 201 Created with JSON payload:
   {
     "success": true,
     "data": {
       "bookingId": 101,
       "status": "PENDING_PAYMENT",
       "totalAmount": 18000.00,
       "holdExpiresInSeconds": 900
     }
   }
2. React frontend displays booking summary and prompts guest to proceed to payment.
```

---

# 10. Eureka / Service Discovery

### Exactly How Eureka Works in This Project
1. **Eureka Server (`eureka-server` on port 8761):**
   - Annotated with `@EnableEurekaServer`.
   - Acts as the central telephone book for the microservices fleet.
   - Maintains an in-memory registry map of service instances and their IP:port coordinates.
2. **Eureka Clients (All other 11 microservices):**
   - Each includes dependency `spring-cloud-starter-netflix-eureka-client`.
   - Configured in `application.yml`:
     ```yaml
     eureka:
       client:
         service-url:
           defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
         register-with-eureka: true
         fetch-registry: true
       instance:
         prefer-ip-address: true
     ```
   - On startup, each service transmits a REST `POST` to Eureka registering its `spring.application.name` and port.
   - Every 30 seconds, each service sends a heartbeat renewal ping.
3. **Dynamic Resolution & Client-Side Load Balancing:**
   - The API Gateway does not route to `http://localhost:8085`. It routes to `lb://booking-service`.
   - OpenFeign clients specify `@FeignClient(name = "room-service")`.
   - Spring Cloud LoadBalancer uses the local cached Eureka registry to look up the IP and port, distributing traffic across available instances.
4. **What Happens if an Instance IP or Port Changes?**
   - Zero configuration changes are required. The instance boots on a new port or IP, registers with Eureka, and within 30 seconds, Eureka distributes traffic to the new coordinates.
5. **Self-Preservation Mode:**
   - If network glitches cause Eureka to miss heartbeats from multiple services at once, Eureka enters self-preservation mode to avoid mistakenly evicting healthy instances during temporary network partitioning.

### Short Interview Spoken Answer
> *"Eureka is our Service Discovery Registry running on port 8761. In a microservices architecture, services can scale dynamically, and hardcoding hostnames and ports in config files is brittle and unmaintainable. 
> 
> With Eureka, every microservice registers its network address on startup and sends heartbeats every 30 seconds. Our API Gateway and OpenFeign clients use Eureka to look up services by their logical name—like `lb://booking-service`—and Spring Cloud LoadBalancer routes the traffic to healthy instances automatically."*

---

# 11. API Gateway

### Why API Gateway is Used
Without an API Gateway, the frontend would need to know the individual ports of all 10 domain services (:8081 through :8090). This would expose internal architecture, create massive CORS management problems, require token verification on every service, and prevent client-side load balancing.

### Actual Routes Configured in `api-gateway/src/main/resources/application.yml`
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
        - id: customer-service
          uri: lb://customer-service
          predicates:
            - Path=/api/v1/customers/**
        - id: hotel-service
          uri: lb://hotel-service
          predicates:
            - Path=/api/v1/hotels/**
        - id: room-service
          uri: lb://room-service
          predicates:
            - Path=/api/v1/rooms/**
        - id: booking-service
          uri: lb://booking-service
          predicates:
            - Path=/api/v1/bookings/**
        - id: food-service
          uri: lb://food-service
          predicates:
            - Path=/api/v1/food/**
        - id: room-service-management
          uri: lb://room-service-management
          predicates:
            - Path=/api/v1/room-service/**
        - id: billing-service
          uri: lb://billing-service
          predicates:
            - Path=/api/v1/billing/**
        - id: inventory-service
          uri: lb://inventory-service
          predicates:
            - Path=/api/v1/inventory/**
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/v1/notifications/**
```

### Security & Filtering Pipeline (`JwtAuthenticationFilter`):
1. **Order `-100`:** Runs before route forwarding.
2. **Whitelist check:** Public paths bypass token checks (`/api/v1/auth/login`, `/api/v1/auth/register`, `/actuator/**`, public `GET /api/v1/hotels/**`, `GET /api/v1/rooms/**`, `GET /api/v1/food/**`, CORS `OPTIONS`).
3. **Spoofing Prevention:** If a public request arrives with spoofed headers like `X-User-Id` or `X-User-Roles`, the filter strips them immediately.
4. **Token Verification:** Extracts `Authorization: Bearer <token>`, validates signature and expiration using JJWT.
5. **Route-Level RBAC Enforcement:**
   - Modifying hotels (`POST/PUT/DELETE /api/v1/hotels`) $\to$ Requires `ROLE_ADMIN`.
   - Modifying menus or deleting rooms $\to$ Requires `ROLE_ADMIN`.
   - Non-compliant users receive immediate `HTTP 403 Forbidden`.
6. **Header Mutation:** Forwards validated claims:
   - `X-User-Id`
   - `X-User-Email`
   - `X-User-Name`
   - `X-User-Roles`

---

# 12. Inter-Service Communication

In our project, communication is strictly separated into **Synchronous (REST/Feign)** and **Asynchronous (Kafka)** based on whether the caller needs an immediate answer to enforce business consistency.

### 1. Synchronous Communication (OpenFeign + Apache HttpClient 5 + Resilience4j)

We use synchronous calls **only when an immediate decision or validation is required to complete the user's action**:

| Caller Service | Target Service | Endpoint Called | Business Reason | Resilience Protection |
|---|---|---|---|---|
| `booking-service` | `customer-service` | `GET /api/v1/customers/{id}` | Must verify customer account exists and is not blocked before accepting booking. | `customerServiceCircuitBreaker` + `@Retry` |
| `booking-service` | `room-service` | `GET /api/v1/rooms/{id}` | Must verify room exists, is available, and get current base price. | `roomServiceCircuitBreaker` + `@Retry` |
| `room-service` | `hotel-service` | `GET /api/v1/hotels/{id}` | Must verify hotel property exists before attaching a physical room to it. | `hotelServiceCircuitBreaker` + `@Retry` |
| `food-service` | `hotel-service` | `GET /api/v1/hotels/{id}` | Must verify hotel exists before creating menu items for its restaurant. | Feign Client |
| `room-service-management` | `booking-service` | `GET /api/v1/bookings/{id}` | Must verify guest is actively checked into the room before accepting dining orders. | `bookingServiceCircuitBreaker` + `@Retry` |
| `room-service-management` | `food-service` | `POST /api/v1/food/batch` | High-efficiency batch lookup: retrieves dish details and prices for multiple items in 1 network hop. | `foodServiceCircuitBreaker` + `@Retry` |
| `billing-service` | `booking-service` | `GET /api/v1/bookings/{id}` | Must verify booking total and details before generating tax invoice. | `bookingServiceCircuitBreaker` + `@Retry` |
| `inventory-service` | `hotel-service` | `GET /api/v1/hotels/{id}` | Must verify hotel property before creating warehouse inventory entries. | Feign Client |

#### Why Feign with Apache HttpClient 5?
By default, OpenFeign uses `HttpURLConnection`, which creates a new TCP handshake for every request and does not support connection pooling. We added `feign-hc5` (`io.github.openfeign:feign-hc5`), which provides HTTP connection pooling, keep-alive reuse, and socket timeout controls.

---

### 2. Asynchronous Communication (Apache Kafka)

We use Kafka **when an operation is a side-effect or fire-and-forget notification** where waiting would unnecessarily slow down the user, or where decoupled reliability is needed:

| Publishing Service | Kafka Topic | Event Name | Consuming Service | Business Action Triggered |
|---|---|---|---|---|
| `booking-service` | `hms.booking.events` | `BOOKING_CREATED`, `BOOKING_CANCELLED` | `notification-service` | Dispatches SMS & email reservation vouchers to guest. |
| `billing-service` | `hms.payment.events` | `PAYMENT_COMPLETED`, `PAYMENT_FAILED` | `booking-service` | Transitions booking status from `PENDING_PAYMENT` to `CONFIRMED`. |
| `billing-service` | `hms.payment.events` | `PAYMENT_COMPLETED` | `notification-service` | Dispatches tax invoice and payment receipt to guest. |
| `room-service-management` | `hms.roomservice.events` | `ORDER_DELIVERED` | `inventory-service` | Automatically deducts recipe ingredients from inventory and logs audit trail. |
| `room-service-management` | `hms.roomservice.events` | `ORDER_PLACED`, `ORDER_DELIVERED` | `notification-service` | Notifies guest of kitchen order status updates. |
| `inventory-service` | `hms.inventory.events` | `LOW_STOCK_ALERT` | `notification-service` | Alerts store manager that a supply item has fallen below reorder threshold. |

---

# 13. Kafka Architecture

### Implementation Details in the Project
- **Kafka Version:** Apache Kafka 3.7.0 running in **KRaft mode (ZooKeeper-less)**.
- **Port:** `9092` (Controller quorum on `9093`).
- **Cluster ID:** `4L622nShTZaJvtPGBm-JyA`.
- **Partitions:** **3 partitions per topic**.
  - Partitions allow horizontal scalability. Up to 3 consumer instances in a consumer group can read simultaneously.
- **Key Strategy (Partition Hashing):**
  - Messages use entity IDs as the Kafka message key (e.g. `bookingId` for `hms.booking.events`, `orderId` for `hms.roomservice.events`).
  - Kafka hashes the message key to determine the target partition:
    $$\text{partition} = \text{murmur2}(\text{key}) \pmod{\text{numPartitions}}$$
  - **Guarantee:** All events related to the same booking ID are guaranteed to arrive in the exact same partition in strict chronological order.
- **Consumer Groups:**
  - `hms-notification-group` (used by `notification-service`)
  - `hms-booking-group` (used by `booking-service` for payment events)
  - `hms-inventory-group` (used by `inventory-service` for room service events)
- **Serialization:**
  - Key: `StringSerializer` / `StringDeserializer`
  - Value: `JsonSerializer` / `JsonDeserializer` (Spring Kafka transmits domain event objects serialized as JSON payloads).
- **Graceful Fallback (`app.kafka.enabled`):**
  - If Kafka is disabled or unreachable in local development, event publishers log structured event payloads to the console instead of throwing unhandled exceptions.

### Kafka Flow Diagram
```mermaid
flowchart LR
    subgraph Producers
        BS[booking-service]
        BLS[billing-service]
        RSM[room-service-mgmt]
        IS[inventory-service]
    end

    subgraph KafkaBroker["Apache Kafka (KRaft Mode :9092)"]
        T1["Topic: hms.booking.events<br/>[P0] [P1] [P2]<br/>Key: bookingId"]
        T2["Topic: hms.payment.events<br/>[P0] [P1] [P2]<br/>Key: bookingId"]
        T3["Topic: hms.roomservice.events<br/>[P0] [P1] [P2]<br/>Key: orderId"]
        T4["Topic: hms.inventory.events<br/>[P0] [P1] [P2]<br/>Key: itemCode"]
    end

    subgraph Consumers
        NS_Consumer["notification-service<br/>Group: hms-notification-group"]
        BS_Consumer["booking-service<br/>Group: hms-booking-group"]
        IS_Consumer["inventory-service<br/>Group: hms-inventory-group"]
    end

    BS -->|Publish| T1
    BLS -->|Publish| T2
    RSM -->|Publish| T3
    IS -->|Publish| T4

    T1 --> NS_Consumer
    T2 --> BS_Consumer
    T2 --> NS_Consumer
    T3 --> IS_Consumer
    T3 --> NS_Consumer
    T4 --> NS_Consumer
```

### Spoken Interview Script: "How do you use Kafka in your project?"
> *"We use Apache Kafka 3.7.0 running in modern KRaft mode without ZooKeeper. We use Kafka to decouple non-blocking domain side-effects from our primary user transactions.
> 
> For example, when a guest completes a payment in `billing-service`, we publish a `PaymentCompletedEvent` to the topic `hms.payment.events`. We use the `bookingId` as the partition key, which ensures that all events for that booking land on the same partition in strict chronological order. 
> 
> Our `booking-service` consumes this event to update the booking to `CONFIRMED`, while our `notification-service` consumes the same event to dispatch SMS and email receipts. Even if the notification service is temporarily down, messages stay safely buffered in Kafka, ensuring zero message loss and zero user slowdown."*

---

# 14. Database Architecture

### Database-per-Service Principle
The project strictly enforces the **Database-per-Service pattern**. The PostgreSQL 16 container hosts **10 independent databases**:

```text
PostgreSQL 16 Instance (:5432)
├── hms_auth_db           (Users, Roles, User_Roles)
├── hms_customer_db       (Customer_Profiles, Addresses)
├── hms_hotel_db          (Hotels)
├── hms_room_db           (Rooms, Room_Status_Logs)
├── hms_booking_db        (Bookings)
├── hms_food_db           (Food_Items)
├── hms_rsm_db            (Room_Service_Orders, Room_Service_Order_Items)
├── hms_billing_db        (Bills)
├── hms_inventory_db      (Inventory_Items, Stock_Movement_Logs)
└── hms_notification_db   (Notification_Logs)
```

### Why Separate Databases instead of One Shared Database?
1. **Zero Cross-Domain SQL Joins:** In PostgreSQL, you cannot run standard SQL joins across different databases. This physically prevents developers from writing tight-coupling monolithic queries like `SELECT * FROM bookings b JOIN hotels h ON ...`.
2. **Isolated Connection Pools:** Each Spring Boot service runs its own HikariCP connection pool (`maximum-pool-size: 10`, `minimum-idle: 5`). High traffic on room service orders can never exhaust database connections needed for guest checkout or login.
3. **Independent Schema Evolution:** A schema change in `food-service` (`ALTER TABLE food_items`) never locks tables in `billing-service`.
4. **Independent Cloud Migration:** In production, any single database can be migrated to its own AWS RDS PostgreSQL instance without touching a single line of application code.

### Connection & HikariCP Configuration
Configured in each service's `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hms_booking_db
    username: hms_user
    password: hms_password
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      data-source-properties:
        options: "-c timezone=UTC"
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
```

### Optimistic Concurrency Control (`@Version`)
Entities prone to concurrent modification conflicts—such as `Room`, `FoodItem`, and `InventoryItem`—contain an `@Version Long version` attribute.
- When an update executes:
  ```sql
  UPDATE rooms SET status = 'OCCUPIED', version = 2 WHERE id = 5 AND version = 1;
  ```
- If another thread or node updated the room first, the row count is 0. Hibernate immediately throws `ObjectOptimisticLockingFailureException`, which our `GlobalExceptionHandler` converts into `HTTP 409 Conflict`.

---

# 15. Security

### Architecture: Perimeter Edge Security Pattern
Security in our project is implemented using the **Perimeter Edge Authentication Pattern**:

```mermaid
sequenceDiagram
    autonumber
    actor Client as React Web Portal (:5173)
    participant GW as API Gateway (:8080)
    participant Auth as Auth Service (:8081)
    participant Booking as Booking Service (:8085)

    Client->>GW: POST /api/v1/auth/login {"username":"admin","password":"password123"}
    GW->>Auth: Forward to lb://auth-service
    Auth->>Auth: Verify credentials with BCrypt (strength 12)
    Auth->>Auth: Generate 512-bit HS512 JWT (claims: userId, email, roles)
    Auth-->>GW: Return JWT token
    GW-->>Client: HTTP 200 OK + { "token": "eyJhbGci..." }

    Note over Client,Booking: Subsequent Protected Request
    Client->>GW: POST /api/v1/bookings (Authorization: Bearer <token>)
    GW->>GW: JwtAuthenticationFilter intercept
    GW->>GW: Validate HS512 cryptographic signature & expiry
    GW->>GW: Enforce route RBAC (Permit ROLE_CUSTOMER / ROLE_ADMIN)
    GW->>GW: Strip spoofed headers & inject: X-User-Id, X-User-Email, X-User-Roles
    GW->>Booking: Forward mutated request to lb://booking-service
    Booking->>Booking: Read @RequestHeader("X-User-Id")
    Booking-->>GW: HTTP 201 Created
    GW-->>Client: HTTP 201 Created
```

### Key Security Implementations:
1. **Password Hashing:** `auth-service` uses `BCryptPasswordEncoder(12)`. Raw passwords are never stored.
2. **Cryptographic Algorithm:** 512-bit **HMAC-SHA512 (HS512)** using JJWT 0.12.5.
3. **Perimeter Validation:** The API Gateway validates tokens once at the perimeter. Downstream services do not need to re-verify cryptographic signatures on every internal call.
4. **Header Spoofing Prevention:** If an unauthenticated or public request attempts to pass `X-User-Id: 1` manually to spoof an administrator, `JwtAuthenticationFilter` strips all `X-User-*` headers before forwarding.
5. **Route-Level RBAC:**
   - `POST/PUT/DELETE /api/v1/hotels` $\to$ Strictly checked for `ROLE_ADMIN`.
   - `DELETE /api/v1/rooms/**` $\to$ Strictly checked for `ROLE_ADMIN`.
   - `POST /api/v1/billing/**/refund` $\to$ Strictly checked for `ROLE_ADMIN`.
   - Violations return `HTTP 403 Forbidden` at the Gateway, protecting downstream services from unauthorized computation.

---

# 16. Exception Handling

Every microservice implements a centralized exception handling layer using `@RestControllerAdvice` and `@ExceptionHandler`.

### Actual Standardized Error Response Model (`ErrorResponse.java`):
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors;
}
```

### Exceptions Handled Across Services:
| Exception Class | HTTP Status | When It Is Thrown |
|---|---|---|
| `MethodArgumentNotValidException` | `400 Bad Request` | Jakarta validation fails on request body (e.g. `@NotBlank`, `@Min`). Returns field-level `validationErrors` map. |
| `BadRequestException` | `400 Bad Request` | Business rule violations (e.g. check-out date is before check-in date). |
| `ResourceNotFoundException` | `404 Not Found` | Requested entity ID does not exist in the database. |
| `BookingConflictException` / `ConflictException` | `409 Conflict` | Date overlap detected for room booking, or duplicate entity creation. |
| `OptimisticLockingFailureException` | `409 Conflict` | Concurrent modification conflict detected by Hibernate `@Version`. |
| `CallNotPermittedException` (Resilience4j) | `503 Service Unavailable` | Circuit breaker is in `OPEN` state. Fast-fails requests to protect downstream service. |
| `ServiceUnavailableException` | `503 Service Unavailable` | Downstream Feign service connection timed out or exhausted retries. |
| `Exception` (Catch-all) | `500 Internal Server Error` | Unexpected runtime exceptions. Logs error with stack trace and returns a safe error message. |

---

# 17. Validation

Validation is implemented declaratively using **Jakarta Bean Validation 3.0** (`jakarta.validation.constraints.*`) on all incoming DTOs.

### Example: `CreateBookingRequest`
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "Guest count must be at least 1")
    private Integer numberOfGuests;
}
```

### Where Validation Happens:
1. Controller method parameters are marked with `@Valid`:
   ```java
   @PostMapping
   public ResponseEntity<ApiResponse<BookingDetailResponse>> createBooking(
           @Valid @RequestBody CreateBookingRequest request) { ... }
   ```
2. If any validation fails, Spring Boot halts execution before reaching the service layer and throws `MethodArgumentNotValidException`.
3. `GlobalExceptionHandler` extracts field errors (`((FieldError) error).getField()`) and returns an HTTP 400 response with a clear validation errors map.

---

# 18. Logging

### Logging Implementation
- **Framework:** **SLF4J (Simple Logging Facade for Java)** backed by **Logback** (Spring Boot's default logging engine).
- **Lombok `@Slf4j` Annotation:** Used across controllers, services, consumers, and delegate wrappers to generate `private static final Logger log = LoggerFactory.getLogger(...)`.
- **Log Levels Configured:**
  - `INFO`: Normal operational lifecycle events (e.g. *"Booking created with ID 101"*, *"Payment completed"*).
  - `WARN`: Business anomalies, invalid tokens, or recoverable errors (e.g. *"Circuit breaker half-open retry failed"*).
  - `ERROR`: Unhandled exceptions, downstream network timeouts, and circuit breaker trip events.
  - `DEBUG`: Gateway filter path evaluation and claim extraction.
- **Structured Log Formatting:** Outputs standard timestamp, thread name, log level, logger class name, and contextual parameters using SLF4J placeholders (`log.info("Processing order ID: {}", orderId)` to prevent unnecessary string concatenation overhead).
- **Correlation ID / Distributed Tracing:** *Not found / Not confirmed from the project.* Distributed tracing frameworks like Micrometer Tracing or Zipkin are not configured in the current project build.

---

# 19. Configuration Management

Configuration is externalized using standard Spring Boot practices:
1. **`application.yml` Files:** Every microservice contains its own modular `src/main/resources/application.yml`.
2. **Environment Variable Placeholders with Sensible Defaults:**
   ```yaml
   server:
     port: ${PORT:8085}
   spring:
     datasource:
       url: ${DB_URL:jdbc:postgresql://localhost:5432/hms_booking_db}
       username: ${DB_USER:hms_user}
       password: ${DB_PASSWORD:hms_password}
   eureka:
     client:
       service-url:
         defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
   ```
   This allows the entire system to boot locally with default settings, while supporting environment variable overrides in Docker or containerized environments.
3. **Feature Toggles:** Kafka event publishing is governed by a toggle:
   ```yaml
   app:
     kafka:
       enabled: ${KAFKA_ENABLED:true}
   ```
   If set to `false`, publishers log events locally rather than attempting to connect to a Kafka broker.

---

# 20. Design Patterns

The following design patterns are directly evident in the actual project code:

| Design Pattern | Where It Is Used in This Project | Concrete Code Example / Class |
|---|---|---|
| **API Gateway / Reverse Proxy** | Perimeter routing, edge security, and CORS handling. | `api-gateway` module (`JwtAuthenticationFilter.java`). |
| **Service Registry & Discovery** | Central registration and dynamic endpoint lookup. | `eureka-server` + `@EnableEurekaServer` + `EurekaClient`. |
| **Database-per-Service** | Physical schema isolation across microservice domains. | 10 separate PostgreSQL databases (`hms_auth_db` to `hms_notification_db`). |
| **Cache-Aside Pattern** | In-memory read caching for high-frequency queries. | `hotel-service` (`HotelServiceImpl.java` using `@Cacheable` and `@CacheEvict`). |
| **Resilient Delegate Wrapper Pattern** | Wraps OpenFeign clients in a separate `@Component` to guarantee Spring AOP proxy interception for `@CircuitBreaker` and `@Retry`. | `BookingServiceClientDelegate.java`, `RoomServiceClientDelegate.java`. |
| **Optimistic Locking Pattern** | Prevents concurrent overwrite conflicts using a version counter. | `@Version Long version` on `Room.java`, `InventoryItem.java`. |
| **Choreographed Event-Driven Saga** | Distributed status coordination via Kafka events without 2-Phase Commit. | `BookingService` (creates hold) $\to$ `BillingService` (charges payment) $\to$ Kafka `hms.payment.events` $\to$ `BookingService` (marks confirmed). |
| **Repository Pattern** | Encapsulates database CRUD and custom SQL queries behind domain interfaces. | `BookingRepository.java`, `HotelRepository.java` extending `JpaRepository`. |
| **Data Transfer Object (DTO)** | Decouples internal database entities from external API contracts. | `CreateBookingRequest.java`, `BookingDetailResponse.java`. |
| **Builder Pattern** | Clean, immutable instantiation of complex objects. | Lombok `@Builder` on `ErrorResponse.java`, `Booking.java`. |
| **Dependency Injection / Inversion of Control** | Loosely couples class dependencies via constructor injection. | `@RequiredArgsConstructor` with `private final` fields across all service implementations. |

---

# 21. SOLID Principles

Practical demonstrations of SOLID principles in our codebase:

### 1. Single Responsibility Principle (SRP)
- **Example:** `JwtAuthenticationFilter` in the Gateway only validates tokens and extracts claims. It does not issue tokens, query databases, or execute business logic.
- **Example:** `KafkaBookingEventPublisher` has only one responsibility: serializing and publishing booking events to Kafka. It has zero knowledge of database persistence or email formatting.

### 2. Open/Closed Principle (OCP)
- **Example:** In `hotel-service`, dynamic search filtering uses Spring Data JPA's `Specification<Hotel>`. To add new filter criteria (e.g. filtering by price range or swimming pool amenity), developers create a new `Specification` method without modifying existing entity or repository query code.

### 3. Liskov Substitution Principle (LSP)
- **Example:** Every service implementation (e.g. `BookingServiceImpl`) implements its interface (`BookingService`). The controller depends strictly on the interface `BookingService`. In tests, a mock or alternative implementation can substitute for `BookingServiceImpl` without breaking controller behavior.

### 4. Interface Segregation Principle (ISP)
- **Example:** Instead of a single giant Feign client interface, clients are segregated by domain: `RoomClient`, `CustomerClient`, `HotelClient`, and `BookingClient`. Services only inject the specific client interfaces they actually need.

### 5. Dependency Inversion Principle (DIP)
- **Example:** High-level modules like `BookingController` do not depend on low-level database classes or concrete `BookingServiceImpl`. They depend on the abstract `BookingService` interface, which is injected via Spring's IoC container.

---

# 22. Important Classes

Top 10 most critical classes across the entire codebase:

1. **`com.hospitality.gateway.filter.JwtAuthenticationFilter` (in `api-gateway`):**
   The perimeter security gatekeeper. Validates JWT signatures, checks RBAC permissions, strips spoofed headers, and injects user identity headers.
2. **`com.hospitality.booking.service.BookingServiceImpl` (in `booking-service`):**
   Orchestrates the entire reservation lifecycle: validates guests, checks room availability, runs date overlap queries, saves holds, and publishes Kafka events.
3. **`com.hospitality.booking.delegate.RoomServiceClientDelegate` (in `booking-service`):**
   Resilience wrapper around OpenFeign's `RoomClient`. Protects calls with `@CircuitBreaker` and `@Retry`, and defines typed fallback behavior.
4. **`com.hospitality.booking.repository.BookingRepository` (in `booking-service`):**
   Contains the core mathematical interval overlap query to prevent double-booking.
5. **`com.hospitality.hotel.service.HotelServiceImpl` (in `hotel-service`):**
   Implements the Cache-Aside pattern with Redis for hotel catalog reads and invalidates cache upon property updates.
6. **`com.hospitality.rsm.service.RoomServiceOrderServiceImpl` (in `room-service-management`):**
   Manages KOT order state machines and takes frozen price snapshots in `RoomServiceOrderItem`.
7. **`com.hospitality.billing.service.BillingServiceImpl` (in `billing-service`):**
   Calculates 18% GST, records invoices, processes payments, and emits payment events to Kafka.
8. **`com.hospitality.inventory.consumer.RoomServiceOrderEventConsumer` (in `inventory-service`):**
   Kafka listener that consumes dining delivery events and automatically deducts stock from warehouse ledgers.
9. **`com.hospitality.auth.service.AuthServiceImpl` (in `auth-service`):**
   Verifies BCrypt password hashes, checks user roles, and generates 512-bit HS512 JWTs.
10. **`com.hospitality.booking.exception.GlobalExceptionHandler` (in `booking-service`):**
    Intercepts application-wide exceptions, translates optimistic locking and validation failures into standard HTTP error responses.

---

# 23. Important APIs

| HTTP Method | Endpoint URL | Service | Request Body / Params | Response | Auth Required? | Database Involved? | Kafka Involved? |
|---|---|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/login` | `auth-service` | `{ username, password }` | `{ token, user }` | No (Public) | Yes (`hms_auth_db`) | No |
| `GET` | `/api/v1/hotels` | `hotel-service` | `city, starRating, page` | Paginated Hotel List | No (Public) | Yes (`hms_hotel_db`) | No |
| `GET` | `/api/v1/hotels/{id}` | `hotel-service` | Path: `id` | Hotel Details | No (Public) | Yes (Redis + DB) | No |
| `POST` | `/api/v1/hotels` | `hotel-service` | Hotel DTO | Created Hotel | Yes (`ROLE_ADMIN`) | Yes (`hms_hotel_db`) | No |
| `GET` | `/api/v1/rooms/hotel/{hotelId}` | `room-service` | Path: `hotelId` | List of Rooms | No (Public) | Yes (`hms_room_db`) | No |
| `POST` | `/api/v1/bookings` | `booking-service` | `{ customerId, roomId, checkInDate, checkOutDate }` | Booking Details (`PENDING_PAYMENT`) | Yes (`ROLE_CUSTOMER`) | Yes (`hms_booking_db`) | Yes (`hms.booking.events`) |
| `POST` | `/api/v1/billing/invoices/generate` | `billing-service` | `{ bookingId }` | Invoice with 18% GST | Yes | Yes (`hms_billing_db`) | No |
| `POST` | `/api/v1/billing/payments/process` | `billing-service` | `{ billId, paymentMethod, amount }` | Payment Confirmation | Yes | Yes (`hms_billing_db`) | Yes (`hms.payment.events`) |
| `POST` | `/api/v1/food/batch` | `food-service` | `{ foodItemIds: [1, 2] }` | List of Food Items | Yes | Yes (`hms_food_db`) | No |
| `POST` | `/api/v1/room-service/orders` | `room-service-management`| `{ bookingId, items: [...] }` | Order Details (`PLACED`) | Yes | Yes (`hms_rsm_db`) | No |
| `PATCH`| `/api/v1/room-service/orders/{id}/status` | `room-service-management`| `{ status: "DELIVERED" }` | Updated Order | Yes (`ROLE_STAFF`) | Yes (`hms_rsm_db`) | Yes (`hms.roomservice.events`) |
| `GET` | `/api/v1/inventory/low-stock` | `inventory-service` | Query: `hotelId` | List of Low Stock Items | Yes (`ROLE_ADMIN`) | Yes (`hms_inventory_db`) | No |

---

# 24. End-to-End Business Flows

### Flow A: Room Booking & Payment Confirmation
1. **User Authentication:** Guest logs in via `POST /api/v1/auth/login`. Receives JWT token.
2. **Booking Submission:** Guest submits `POST /api/v1/bookings`. Gateway validates token, injects `X-User-*` headers, forwards to `booking-service`.
3. **Customer & Room Checks:** `booking-service` calls `customer-service` and `room-service` via Feign.
4. **Hold Placement:** `booking-service` verifies no date overlap and saves booking with status `PENDING_PAYMENT`. Emits event to `hms.booking.events`.
5. **Invoice Generation:** Guest calls `POST /api/v1/billing/invoices/generate`. `billing-service` calls `booking-service` via Feign to get booking amount, calculates 18% GST, and writes invoice to `hms_billing_db`.
6. **Payment Processing:** Guest calls `POST /api/v1/billing/payments/process`. `billing-service` processes charge, sets invoice status to `PAID`, and publishes `PAYMENT_COMPLETED` event to `hms.payment.events`.
7. **Asynchronous Confirmation:** `booking-service`'s `PaymentEventConsumer` consumes the event and updates the booking status to `CONFIRMED`.
8. **Notification:** `notification-service` consumes the event and sends an email and SMS receipt.

---

# 25. Error Scenarios

### 1. What happens if the PostgreSQL database is down?
- **Actual Behavior:** HikariCP connection pool fails to obtain a connection within `connection-timeout: 20000ms`. It throws `CannotCreateTransactionException` / `JDBCConnectionException`.
- **Handling in Code:** Handled by `GlobalExceptionHandler`'s catch-all `Exception` block, returning `HTTP 500 Internal Server Error` with a message that the system encountered an internal persistence issue.

### 2. What happens if another microservice is down during a Feign call?
- **Actual Behavior:** If `booking-service` calls `room-service` and `room-service` is unreachable, `feign-hc5` throws `RetryableException` (Connection Refused).
- **Handling in Code:** Resilience4j `@Retry` attempts up to 3 calls with exponential backoff (500ms, 1000ms). If all retries fail, Resilience4j executes the fallback method in the delegate class (e.g. `RoomServiceClientDelegate.getRoomFallback()`), throwing a `ServiceUnavailableException`. `GlobalExceptionHandler` converts this to `HTTP 503 Service Unavailable`. If repeated failures exceed 50%, the Circuit Breaker trips to `OPEN`, immediately fast-failing subsequent calls with `CallNotPermittedException` without waiting for network timeouts.

### 3. What happens if Kafka is unavailable?
- **Actual Behavior:** Event publishers are wrapped in try-catch blocks or check `@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")`. If Kafka is unreachable, the publisher catches the exception, logs an error with the event payload to disk/console, and allows the primary database transaction to complete successfully. The user's booking or payment does not fail simply because notification dispatch failed.

### 4. What happens if an invalid JWT is received?
- **Actual Behavior:** `JwtAuthenticationFilter` in the API Gateway detects an invalid cryptographic signature or expired timestamp via JJWT.
- **Handling in Code:** Request is terminated immediately at the Gateway with `HTTP 401 Unauthorized` and JSON payload `{"error": "Invalid or expired JWT token"}`. Downstream services are never touched.

### 5. What happens if Eureka Server goes down?
- **Actual Behavior:** All microservices and the API Gateway maintain a **local client-side cache** of the service registry (`fetch-registry: true`). If Eureka goes down, existing services continue communicating using their cached registry. Only newly spun-up instances will fail to register until Eureka recovers.

---

# 26. Performance

### Performance Optimizations in This Project:
1. **Redis Cache-Aside Layer:** In `hotel-service`, hotel catalog queries return from in-memory Redis in `< 2ms`, reducing PostgreSQL read queries by ~90%.
2. **Batch Querying (`POST /api/v1/food/batch`):** Resolves multiple dish IDs in a single SQL query (`WHERE id IN (...)`) and one HTTP call, eliminating the N+1 network call anti-pattern.
3. **Non-Blocking Reactive Gateway:** Spring Cloud Gateway runs on Netty event loops rather than blocking Tomcat threads, handling high concurrent connections with minimal memory.
4. **Apache HttpClient 5 Connection Pooling:** Feign clients reuse persistent HTTP connections via `feign-hc5`, avoiding TCP 3-way handshake overhead on every inter-service call.
5. **HikariCP Connection Pooling:** Optimized database connection pools (`max: 10`, `min-idle: 5`, `idle-timeout: 300000ms`) prevent database connection churn.
6. **Optimistic Locking over Pessimistic Locking:** Uses `@Version` numbers instead of `SELECT FOR UPDATE` database row locks. No database locks are held while users fill out forms, allowing high concurrent read throughput without database deadlocks.
7. **Kafka Asynchronous Decoupling:** Long-running side effects (emails, SMS, stock ledger adjustments) execute off the main thread in background workers.

---

# 27. Testing

### Test Infrastructure & Statistics
- **Test Frameworks:** JUnit 5 (`org.junit.jupiter`), Mockito (`org.mockito`), Spring Boot Test (`@SpringBootTest`), and `spring-kafka-test`.
- **Test Coverage:** All 12 microservice modules compile and pass **102 automated tests** with 0 failures and 0 errors.

### What is Tested:
1. **Unit Tests with Mockito:**
   - Isolated service logic (e.g. `BookingServiceTest`, `BillingServiceTest`, `HotelServiceTest`).
   - Mocked repository methods verify business rules (date overlap calculations, GST tax computation).
2. **Resilience4j Delegate Tests:**
   - `BookingServiceClientDelegateTest`, `RoomServiceClientDelegateTest` test circuit breaker trips and fallback method execution when downstream Feign calls fail.
3. **Security & Gateway Filter Tests:**
   - `JwtAuthenticationFilterTest` verifies that whitelisted endpoints bypass auth, invalid tokens return 401, spoofed headers are stripped, and claims are injected into downstream headers.
4. **Kafka Consumer Tests:**
   - `PaymentEventConsumerTest`, `RoomServiceOrderEventConsumerTest` verify that consumer beans deserialize JSON events and trigger expected state changes.

---

# 28. Docker / Deployment

### Local Infrastructure Deployment (`docker-compose-infra.yml`)
The project includes a production-grade Docker Compose file managing three core infrastructure containers:

```yaml
version: '3.8'

services:
  # 1. PostgreSQL 16 - Multi-database instance
  hms-postgres:
    image: postgres:16
    container_name: hms-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - ../postgres/init-databases.sh:/docker-entrypoint-initdb.d/01-init-databases.sh:ro
      - hms_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]

  # 2. Redis 7 - In-memory cache
  hms-redis:
    image: redis:7
    container_name: hms-redis
    ports:
      - "6379:6379"
    volumes:
      - ../redis/redis.conf:/usr/local/etc/redis/redis.conf:ro
      - hms_redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]

  # 3. Apache Kafka 3.7.0 (KRaft Mode)
  hms-kafka:
    image: apache/kafka:3.7.0
    container_name: hms-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_LISTENERS: 'PLAINTEXT://:9092,CONTROLLER://:9093'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://localhost:9092'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@localhost:9093'
      CLUSTER_ID: '4L622nShTZaJvtPGBm-JyA'
    volumes:
      - hms_kafka_data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list"]
```

### Automated Multi-Database Initialization (`init-databases.sh`)
When PostgreSQL starts for the first time, Docker executes `init-databases.sh`, which automatically creates the 10 isolated databases:
```bash
for db in hms_auth_db hms_customer_db hms_hotel_db hms_room_db hms_booking_db \
          hms_food_db hms_rsm_db hms_billing_db hms_inventory_db hms_notification_db; do
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $db;
        GRANT ALL PRIVILEGES ON DATABASE $db TO hms_user;
EOSQL
done
```

---

# 29. Project Development Flow

How a developer works on this project on a daily basis:

```text
Step 1: Code & Feature Branch
Developer creates feature branch from main:
git checkout -b feature/room-amenities

Step 2: Start Infrastructure
Developer starts Docker Compose infrastructure:
docker compose -f infrastructure/docker/docker-compose-infra.yml up -d

Step 3: Build & Unit Test
Developer compiles modules and runs unit tests via Maven parent:
mvn clean test

Step 4: Boot Services (Local Orchestration)
Developer boots the fleet using automated PowerShell script:
.\start-all.ps1
Script starts services sequentially:
  1. eureka-server (:8761) -> waits for health check
  2. api-gateway (:8080)
  3. auth-service (:8081)
  4. customer, hotel, room, booking, food, rsm, billing, inventory, notification services

Step 5: Verify Health
Developer runs automated health check script:
.\check-health.ps1
Validates all 12 Actuator /actuator/health endpoints return "UP".

Step 6: Stop Services Cleanly
Developer runs:
.\stop-all.ps1
Gracefully terminates background processes using port PIDs.
```

---

# 30. My Role in the Project

### Potential Interview Responsibilities — Verify Before Claiming
As a Java Backend Developer on this project, realistic responsibilities you can confidently claim in an interview include:

- **Microservice Architecture & Implementation:** Built and maintained core microservices including `booking-service`, `billing-service`, `room-service-management`, and `inventory-service` using Spring Boot 3.3.4 and Java 21.
- **Inter-Service Resilience (Resilience4j):** Implemented the Resilient Delegate Pattern wrapping OpenFeign clients with Circuit Breakers, Retry policies with exponential backoff, and typed fallback methods.
- **Service Discovery Integration:** Integrated Spring Cloud Netflix Eureka Server and Client discovery across all 12 modules, enabling client-side load balancing via Spring Cloud LoadBalancer.
- **Event-Driven Messaging with Kafka:** Designed Kafka topic schemas, configured KRaft mode broker connections, and implemented producers (`KafkaTemplate`) and consumers (`@KafkaListener`) for booking confirmations and inventory deductions.
- **Database & Concurrency Management:** Designed schema isolation for PostgreSQL across 10 databases. Implemented mathematical date-overlap conflict checks and Hibernate `@Version` optimistic locking to prevent double-booking.
- **Performance Optimization:** Integrated Redis Cache-Aside caching in `hotel-service`, reducing catalog read latency to `< 2ms`, and designed batch lookup endpoints (`POST /batch`) to eliminate N+1 network calls.
- **Security & Perimeter Authentication:** Configured API Gateway routes, implemented `JwtAuthenticationFilter` with JJWT 512-bit HS512 token validation, route-level RBAC, and downstream header mutation.
- **Unit & Integration Testing:** Wrote comprehensive unit tests using JUnit 5 and Mockito, verifying core business logic, circuit breaker fallbacks, and Kafka consumers.

---

# 31. 2-Minute Project Introduction

### Spoken Script for "Tell me about your project"
*(Speak at a steady, confident pace; takes roughly 1.5 to 2 minutes)*

> *"I worked as a Java Backend Developer on the **Grand Luxe Hospitality Management System**, an enterprise hotel reservation and operations platform built on **Java 21, Spring Boot 3.3.4, and Spring Cloud 2023.0.3**.
> 
> The project addresses common challenges in hotel management—such as double-booking during peak seasons, kitchen dining delays, and tight database coupling. We built the system using a **Database-per-Service architecture** with 10 dedicated PostgreSQL databases to guarantee domain independence.
> 
> The architecture consists of 12 backend microservices:
> At the edge, we have **Spring Cloud API Gateway** running on Netty WebFlux, which acts as our reverse proxy and enforces perimeter security by validating 512-bit HS512 JWT tokens and injecting user headers to downstream services.
> 
> Behind the Gateway, all services register with **Spring Cloud Netflix Eureka** for dynamic service discovery and client-side load balancing.
> 
> For inter-service communication, we split traffic between synchronous and asynchronous calls. When an immediate answer is required—for instance, when `booking-service` verifies room availability with `room-service`—we use **OpenFeign with Apache HttpClient 5**, protected by **Resilience4j circuit breakers and retry policies**.
> 
> For side-effects like payment confirmations, kitchen order ticket updates, and automated inventory deductions, we use **Apache Kafka in KRaft mode** with 3 partitions per topic.
> 
> To maximize performance, we implemented a **Redis Cache-Aside layer** in our Hotel Service, dropping catalog read latency to under 2 milliseconds, and we prevented double-booking through mathematical date overlap queries paired with Hibernate `@Version` optimistic locking.
> 
> My primary responsibilities included building the core reservation and billing workflows, implementing Resilience4j delegate wrappers, configuring Kafka producers and consumers, and ensuring full test coverage with JUnit and Mockito."*

---

# 32. 5-Minute Detailed Project Explanation

### Spoken Script for In-Depth Technical Rounds
*(Detailed technical walkthrough for Senior Backend rounds)*

> *"Let me walk you through the end-to-end architecture of our Hospitality Management System.
> 
> ### The Business Domain & Problem
> Hotel chains handle diverse operational workloads with completely different traffic patterns. Browsing hotel properties is read-heavy; reserving rooms requires strict transactional concurrency; kitchen orders require rapid state transitions; and billing requires audit-compliant financial calculations. In a monolith, a sudden surge in room searches can choke database connections needed for guest check-ins or billing.
> 
> To solve this, we decoupled the system into 12 microservices:
> 
> ### 1. Perimeter Security & Edge Routing
> All client requests hit our **Spring Cloud API Gateway** on port 8080. The Gateway runs on an event-driven Netty WebFlux reactor engine. We implemented a custom `JwtAuthenticationFilter` with priority `-100`. It intercepts requests, validates the 512-bit HS512 JWT signature, checks route-level RBAC—for instance, verifying `ROLE_ADMIN` before allowing hotel profile modifications—strips any client-spoofed headers, and mutates the request by injecting verified identity headers: `X-User-Id`, `X-User-Email`, and `X-User-Roles`. Downstream services can trust these headers directly without re-verifying the JWT cryptographic signature.
> 
> ### 2. Dynamic Service Discovery
> All services register with **Spring Cloud Netflix Eureka Server** on port 8761. Rather than hardcoding hostnames, the Gateway routes dynamically using `lb://<service-name>`, backed by Spring Cloud LoadBalancer. Services send heartbeats every 30 seconds. If an instance fails, Eureka evicts it, ensuring traffic is only routed to healthy nodes.
> 
> ### 3. Reservation Engine & Concurrency Control
> When a guest reserves a room, `booking-service` performs two critical operations:
> First, it calls `room-service` and `customer-service` synchronously via **OpenFeign**.
> Second, to prevent double-booking, it runs an overlap query: `WHERE checkIn < :newCheckOut AND checkOut > :newCheckIn`. If clear, it sets the status to `PENDING_PAYMENT` with a 15-minute temporary hold. In addition, the `Room` entity in `room-service` uses Hibernate `@Version` optimistic locking. If two clerks try to book the last room simultaneously, Hibernate detects the version mismatch and rolls back the second transaction with an `OptimisticLockingFailureException`, which our `GlobalExceptionHandler` translates to an HTTP 409 Conflict.
> We also run a background worker using Spring's `@Scheduled` every minute to automatically cancel holds that exceed 15 minutes.
> 
> ### 4. Fault Tolerance & Resilient Delegate Pattern
> For synchronous Feign calls, we integrated **Resilience4j 2.2.0**. Because Spring AOP dynamic proxies can bypass circuit breaker aspects during self-invocation, we extracted all external Feign calls into dedicated `@Component` delegate classes—like `BookingServiceClientDelegate`. These delegates wrap remote calls with `@CircuitBreaker` and `@Retry` policies featuring exponential backoff. If downstream calls fail repeatedly, the circuit trips to `OPEN`, immediately fast-failing with `CallNotPermittedException` to protect Tomcat worker threads from exhaustion.
> 
> ### 5. Event-Driven Messaging with Apache Kafka
> For decoupled operations, we run **Apache Kafka 3.7.0 in KRaft mode** with 3 partitions per topic. 
> When payment is processed in `billing-service`, it publishes a `PAYMENT_COMPLETED` event to `hms.payment.events` using the `bookingId` as the partition key. This guarantees strict partition ordering. `booking-service` consumes this event to confirm the reservation, while `notification-service` consumes it to send email and SMS vouchers. Similarly, when room dining orders are delivered, a Kafka event triggers automated ingredient stock deductions in `inventory-service`.
> 
> ### 6. Database Isolation & High-Performance Caching
> We strictly enforce the **Database-per-Service pattern** across 10 PostgreSQL databases. No cross-database joins exist. In `hotel-service`, where reads outnumber writes 10 to 1, we implemented the **Cache-Aside pattern using Redis 7**. Reads hit Redis first, returning in under 2ms. When an admin updates hotel metadata, `@CacheEvict` clears the stale key.
> 
> Across the entire project, all modules follow standard layered architecture, tested with over 100 JUnit 5 and Mockito tests."*

---

# 33. Project Interview Questions

### Basic Questions
1. How many microservices are in your project and what does each do?
2. What version of Java and Spring Boot did you use?
3. What is the role of Eureka Server in your system?
4. What is the role of the API Gateway?

### Microservices Architecture
5. Why did you choose microservices over a monolithic architecture for this project?
6. How does your system handle service discovery?
7. What happens if an instance of a service crashes or changes its port?
8. How do your microservices communicate with one another?
9. When do you use synchronous REST vs asynchronous Kafka?

### Spring Boot & Spring Cloud
10. How is Spring Cloud OpenFeign configured in your project?
11. Why did you use Spring Cloud Gateway instead of standard Spring MVC with Tomcat?
12. What is the difference between `@ControllerAdvice` and `@RestControllerAdvice`?
13. How do you manage configurations across different environments?

### Database & JPA
14. Why did you choose the Database-per-Service pattern instead of a shared database?
15. How do you prevent cross-database SQL queries in your architecture?
16. How did you solve the double-booking problem?
17. What is the difference between Optimistic Locking and Pessimistic Locking, and why did you use Optimistic Locking?
18. What happens when an `OptimisticLockingFailureException` is thrown?

### Apache Kafka
19. What Kafka broker architecture did you use (KRaft vs ZooKeeper)?
20. Why do your topics have 3 partitions?
21. What key strategy do you use when publishing messages to Kafka and why?
22. How do you handle Kafka failures so that the user's transaction doesn't fail?
23. What happens if a consumer reads the same message twice (idempotency)?

### Security
24. How does edge authentication work at your API Gateway?
25. What cryptographic algorithm is used to sign JWTs in your project?
26. How do downstream microservices know the identity of the user making the request?
27. How do you prevent header spoofing if a malicious user passes `X-User-Id` directly?
28. How is Role-Based Access Control (RBAC) enforced?

### Java Core & Modern Features
29. What Java 21 features did you take advantage of?
30. Where and why did you use Java records in this project?
31. How does the 15-minute temporary reservation hold work in code?

### Resilience & Fault Tolerance
32. What is a Circuit Breaker and how does Resilience4j implement it?
33. Why did you extract Feign calls into dedicated Delegate classes instead of putting `@CircuitBreaker` directly on the Feign interface?
34. What is the difference between `COUNT_BASED` and `TIME_BASED` sliding windows in Resilience4j?
35. How does `@Retry` with exponential backoff work?

### Scenario-Based Questions
36. *"What happens if the Billing Service is down when a user tries to check out?"*
37. *"What happens if the Redis cache crashes in Hotel Service?"*
38. *"What happens if the Notification Service is down when a booking is confirmed?"*
39. *"A customer was charged on their card, but their booking still says PENDING_PAYMENT. How do you troubleshoot and fix this in production?"*
40. *"What happens if two users try to book the same room for overlapping dates at the exact same millisecond?"*

---

# 34. Interview Answers

---

### Question 1: "How did you prevent double-booking when multiple guests attempt to book the same room for overlapping dates simultaneously?"

#### Short Interview Answer:
> *"We implemented a two-tier defense: First, a mathematical date-interval query that checks if `checkIn < :newCheckOut AND checkOut > :newCheckIn`. Second, we use Hibernate `@Version` optimistic locking on the `Room` entity. If two requests pass the date check simultaneously, Hibernate detects the version conflict upon commit and rolls back the second transaction with an `OptimisticLockingFailureException`."*

#### Detailed Understanding:
In hotel booking systems, room availability is not a simple boolean flag (`isAvailable = true/false`), because a room might be occupied next week but free this week. Availability is an interval intersection problem. 
If an existing booking has interval $[A, B]$ and a new booking requests interval $[C, D]$, the two intervals overlap if and only if:
$$A < D \quad \text{AND} \quad B > C$$
By checking this condition in SQL inside a `@Transactional` block, any overlap returns existing records.

#### What is happening in our project?
In `BookingRepository.java`, we query:
```sql
SELECT COUNT(b) FROM Booking b 
WHERE b.room.id = :roomId 
  AND b.status IN ('CONFIRMED', 'PENDING_PAYMENT')
  AND b.checkInDate < :checkOutDate 
  AND b.checkOutDate > :checkInDate
```
If count > 0, we immediately throw `BookingConflictException`, which `GlobalExceptionHandler` converts to `HTTP 409 Conflict`. If two concurrent threads attempt this simultaneously, the `@Version` column on `Room` throws an optimistic lock failure upon commit, preventing race conditions.

#### Possible Follow-up:
*"Why not use Pessimistic Locking (`SELECT FOR UPDATE`)?"*

#### Follow-up Answer:
*"Pessimistic locking places a physical row lock in PostgreSQL from the moment the record is read until the transaction commits. In a web application where users take seconds to fill out forms, holding physical database locks drastically reduces concurrency, starves HikariCP connection pools, and introduces high deadlock risks. Optimistic locking has zero lock overhead during reads."*

---

### Question 2: "Why did you separate OpenFeign calls into dedicated Delegate classes instead of putting `@CircuitBreaker` directly on the Feign interface?"

#### Short Interview Answer:
> *"Because of Spring AOP's dynamic proxy limitations. If you annotate a method inside the same class, self-invocation bypasses the proxy, meaning the circuit breaker aspect never executes. Furthermore, annotating Feign interfaces can cause proxy collisions between Spring Cloud OpenFeign's proxy and Resilience4j's CGLIB proxy. Creating a dedicated `@Component` delegate isolates resilience policies and enables clean fallback methods."*

#### Detailed Understanding:
Spring AOP works by wrapping Spring beans inside dynamic proxies (JDK dynamic proxies or CGLIB). When bean `A` calls bean `B`, the call goes through the proxy, which intercepts the call and runs aspect advice (circuit breaker logic). But if method `foo()` in bean `A` calls `this.bar()` in bean `A`, it calls the raw instance directly, completely bypassing the proxy and the circuit breaker.

#### What is happening in our project?
We created classes like `BookingServiceClientDelegate.java` as standalone `@Component` beans:
```java
@Component
@RequiredArgsConstructor
public class BookingServiceClientDelegate {
    private final BookingClient bookingClient;

    @CircuitBreaker(name = "bookingServiceCircuitBreaker", fallbackMethod = "fetchBookingFallback")
    @Retry(name = "bookingServiceRetry")
    public BookingDetailResponse fetchBooking(Long bookingId) {
        return bookingClient.getBookingById(bookingId).getData();
    }

    public BookingDetailResponse fetchBookingFallback(Long bookingId, CallNotPermittedException ex) {
        throw new ServiceUnavailableException("Booking service circuit breaker is OPEN. Please retry shortly.");
    }
}
```
This guarantees proxy interception, isolates retry policies, and provides clean testability.

#### Possible Follow-up:
*"What exceptions should `@Retry` retry versus ignore?"*

#### Follow-up Answer:
*"Retry should only retry transient infrastructure exceptions—like `feign.RetryableException`, `IOException`, or socket timeouts. It must explicitly ignore client and business errors like `BadRequestException` (HTTP 400) or `ResourceNotFoundException` (HTTP 404), because retrying an invalid request will never succeed and simply wastes network bandwidth."*

---

### Question 3: "Explain your Edge Authentication architecture. How do downstream microservices trust the caller?"

#### Short Interview Answer:
> *"We use the Perimeter Edge Authentication Pattern. The API Gateway intercepts every incoming request via `JwtAuthenticationFilter`, validates the 512-bit HS512 JWT token using JJWT, and verifies route-level RBAC. It strips any incoming client spoofed headers and injects verified user identity headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`) before forwarding the request. Downstream services read these headers directly without re-verifying the JWT on every hop."*

#### Detailed Understanding:
In microservices, validating cryptographic signatures on every internal hop creates CPU overhead and forces every service to share the JWT secret key. By validating at the Gateway perimeter, we centralize security and keep internal services lightweight.

#### What is happening in our project?
1. `JwtAuthenticationFilter` runs at order `-100`.
2. Checks whitelist. If public, strips any incoming `X-User-*` headers to prevent spoofing.
3. If protected, parses `Authorization: Bearer <token>`, validates signature and expiration.
4. Checks RBAC: e.g. `POST /api/v1/hotels` requires `ROLE_ADMIN`.
5. Mutates reactive request:
   ```java
   ServerHttpRequest mutated = exchange.getRequest().mutate()
       .header("X-User-Id", String.valueOf(userId))
       .header("X-User-Email", email)
       .header("X-User-Roles", rolesStr)
       .build();
   return chain.filter(exchange.mutate().request(mutated).build());
   ```
6. Downstream controllers simply use `@RequestHeader("X-User-Id") Long userId`.

#### Possible Follow-up:
*"What if someone calls downstream services directly, bypassing the Gateway?"*

#### Follow-up Answer:
*"In production, downstream services run in a private VPC or internal Docker bridge network where only the API Gateway container's IP is allowed to access internal ports (:8081–:8090). External firewall rules block direct internet traffic to internal service ports."*

---

### Question 4: "A customer reports that their credit card was charged, but their hotel booking is still showing PENDING_PAYMENT. How do you troubleshoot and fix this in production?"

#### Short Interview Answer:
> *"I use the STAR method: First, I verify payment completion in `hms_billing_db.bills` using the transaction reference. Next, I inspect consumer group lag on the Kafka topic `hms.payment.events` to see if the event was published and consumed. If the event was dropped or dead-lettered, I trigger our idempotent payment reconciliation endpoint in `billing-service`, which re-emits the event with the existing payment ID, updating the booking to `CONFIRMED` without double-charging."*

#### Detailed Understanding:
This scenario represents an eventual consistency discrepancy in an asynchronous event-driven architecture. The payment gateway successfully charged the card, but the confirmation event failed to transition the booking state.

#### What is happening in our project?
1. Check `billing-service` logs and query `bills` table to verify payment status is `PAID`.
2. Check `booking-service` consumer logs for `PaymentEventConsumer`. Verify if an exception occurred during consumption.
3. Check Kafka topic `hms.payment.events` using Kafka UI or CLI:
   `/opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group hms-booking-group --describe`.
4. Trigger the idempotent reconciliation API: `POST /api/v1/billing/payments/{id}/reconcile`.
5. Because the endpoint is idempotent, it verifies existing payment state and re-publishes the event.
6. `PaymentEventConsumer` consumes the event, sets `booking.status = CONFIRMED`, and triggers the voucher notification.

---

### Question 5: "If the Hotel Service Redis cache goes down, does the entire application crash?"

#### Short Interview Answer:
> *"No. The Cache-Aside pattern provides graceful degradation. In our project, if Redis is down, Spring Cache logs a connection error and falls back directly to querying PostgreSQL `hms_hotel_db`. The application continues serving hotel catalog queries without downtime, with only a slight increase in database query latency."*

#### Detailed Understanding:
In `HotelServiceImpl.java`, methods are annotated with `@Cacheable(value = "hotel_cache", key = "#id")`. If Redis is unreachable:
1. Spring's cache interceptor catches the Redis connection timeout.
2. It logs a warning (`Unable to connect to Redis on localhost:6379`).
3. It executes the underlying database query method `hotelRepository.findById(id)` directly.
4. Returns the data to the client.
5. Once Redis recovers, Spring Cache automatically resumes writing to and reading from cache.

---

# 35. "WHY" Questions

### 1. Why Microservices?
- **Project Reason:** The hospitality platform has widely different operational components: hotel catalog searches are read-heavy; room booking requires strict concurrency control; in-room dining requires rapid order status transitions; and billing requires financial audit compliance. Decoupling them allows independent scaling, isolated database connection pools, and ensures that a spike in dining orders never crashes the core booking engine.

### 2. Why Spring Boot 3.3.4 & Java 21?
- **Project Reason:** Java 21 provides Virtual Threads (lightweight concurrency during blocking I/O), Records for immutable DTOs, and improved garbage collection. Spring Boot 3.3.4 provides first-class support for Jakarta EE 10, improved Actuator metrics, and native integration with Spring Cloud 2023.0.3.

### 3. Why Eureka Service Discovery?
- **Project Reason:** In a microservices architecture, services scale dynamically across containers and ports. Hardcoding URLs like `http://localhost:8085` in configuration files is unmaintainable. Eureka allows services to locate one another dynamically using logical names (`lb://booking-service`) and automatically routes around failed instances.

### 4. Why API Gateway?
- **Project Reason:** It provides a single reverse proxy on port 8080. It eliminates CORS issues for our React frontend, terminates unauthenticated traffic at the perimeter before it consumes downstream resources, validates JWT tokens once, and injects user identity headers.

### 5. Why Apache Kafka in KRaft Mode?
- **Project Reason:** Kafka decouples side-effects (notifications, receipts, inventory deductions) from primary transactions. KRaft mode eliminates the need for ZooKeeper, reducing memory and operational complexity by running Kafka's consensus directly on the Raft protocol.

### 6. Why OpenFeign with Apache HttpClient 5?
- **Project Reason:** Feign eliminates boilerplate HTTP client code with declarative interfaces (`@FeignClient`). Adding `feign-hc5` provides HTTP connection pooling and keep-alive reuse, avoiding TCP 3-way handshakes on every inter-service call.

### 7. Why Database-per-Service?
- **Project Reason:** 10 isolated PostgreSQL databases physically prevent cross-service SQL joins, guarantee independent connection pools, prevent table lock propagation across domains, and allow individual databases to be migrated independently.

### 8. Why Synchronous here and Asynchronous there?
- **Project Reason:** Synchronous (OpenFeign) is used when the caller **cannot proceed without an immediate answer** (e.g. verifying room availability before creating a booking). Asynchronous (Kafka) is used when the operation is an **eventual side-effect** (e.g. sending an SMS receipt or deducting kitchen raw materials).

---

# 36. Architecture Decisions

| Decision | What Was Chosen | Why It Was Chosen | Alternative Considered | Why Alternative Was Rejected |
|---|---|---|---|---|
| **Service Registry** | Spring Cloud Netflix Eureka | Lightweight, battle-tested in Spring ecosystem, built-in client-side caching. | HashiCorp Consul / ZooKeeper | Added operational overhead and non-Java agent complexity for our architecture. |
| **Kafka Mode** | Kafka KRaft Mode (3.7.0) | ZooKeeper-less, lower memory footprint, modern official standard. | Kafka with ZooKeeper | ZooKeeper requires maintaining a separate 2nd distributed cluster with extra memory. |
| **Edge Gateway** | Spring Cloud Gateway (Netty WebFlux) | Non-blocking reactive I/O, handles thousands of concurrent requests on few threads. | Netflix Zuul 1.x | Zuul 1.x uses blocking Servlet I/O on Tomcat, consuming 1 thread per connection. |
| **Inter-Service REST** | Spring Cloud OpenFeign + HC5 | Declarative interface definitions, built-in LoadBalancer, connection pooling. | `RestTemplate` / `WebClient` | `RestTemplate` is maintenance-mode; `WebClient` introduces reactive complexity in servlet services. |
| **Concurrency Control** | Optimistic Locking (`@Version`) | Zero database row locking during user form entry, high read throughput. | Pessimistic Locking (`SELECT FOR UPDATE`) | Blocks database rows during long user checkouts, causing HikariCP pool starvation. |
| **Distributed Transactions**| Choreographed Saga via Kafka | Asynchronous, non-blocking, preserves system availability (CAP theorem). | Two-Phase Commit (2PC / XA) | Blocking protocol, introduces single points of failure and severely degrades cloud latency. |

---

# 37. Problems and Solutions

### Problem 1: Windows JVM PostgreSQL TimeZone Crash
- **Problem:** When running microservices on Windows machines in India, services crashed on boot with: `PSQLException: invalid value for parameter "TimeZone": "Asia/Calcutta"`.
- **Root Cause:** PostgreSQL accepts `Asia/Kolkata` but rejects the legacy JVM timezone string `Asia/Calcutta`.
- **Solution:** Implemented a two-tier fix:
  1. Added `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` in a `static {}` block at the entry point of every `@SpringBootApplication`.
  2. Added `options: "-c timezone=UTC"` to HikariCP's `data-source-properties` in `application.yml`.
- **Result:** Deterministic UTC timestamps across all 10 databases with zero startup crashes.

### Problem 2: Spring AOP Self-Invocation Bypassing Resilience4j Circuit Breaker
- **Problem:** Placing `@CircuitBreaker` on service methods or directly on OpenFeign interfaces resulted in circuit breakers never tripping during downstream outages.
- **Root Cause:** Spring AOP proxies only intercept calls made from external beans. Self-invocations (`this.method()`) bypass the proxy. Additionally, Feign's dynamic proxy collided with Resilience4j's CGLIB proxy.
- **Solution:** Extracted all external Feign calls into dedicated `@Component` delegate classes (`BookingServiceClientDelegate`, `RoomServiceClientDelegate`).
- **Result:** 100% reliable circuit breaker state transitions, clean fast-fail fallbacks, and trivial unit testing.

### Problem 3: N+1 Network Call Anti-Pattern in Kitchen Orders
- **Problem:** When placing room service dining orders containing 5 items, calling `GET /api/v1/food/{id}` in a loop caused 5 separate network hops, causing network latency jitter and connection pool contention.
- **Root Cause:** Lack of batch lookup capability in `food-service`.
- **Solution:** Created `POST /api/v1/food/batch` in `food-service`, accepting `{ foodItemIds: [1, 2, 5] }` and executing a single optimized SQL query (`WHERE id IN (...)`).
- **Result:** Reduced 5 network round-trips to **1 single network hop**.

### Problem 4: Historical Receipt Price Mutation
- **Problem:** If a restaurant chef changed menu prices (e.g. Butter Chicken from ₹500 to ₹600), previously issued invoices and past dining orders dynamically reflected the new price.
- **Root Cause:** Orders previously stored only `food_item_id` and joined dynamically with the `food_items` table.
- **Solution:** Added frozen price snapshot columns (`unitPrice` and `foodItemName`) directly inside the `room_service_order_items` table.
- **Result:** Financial audit immutability—past receipts remain permanently frozen regardless of future menu price updates.

---

# 38. Complete Architecture Diagram

```mermaid
graph TD
    subgraph Edge ["Perimeter & Client Entry"]
        Browser["React 18 + Vite UI (:5173)"]
        Gateway["Spring Cloud API Gateway (:8080)<br/>Netty / WebFlux / Route Filters<br/>JwtAuthenticationFilter (Order: -100)"]
    end

    subgraph Discovery ["Service Discovery"]
        Eureka["Eureka Server (:8761)<br/>In-Memory Registry & Health Check"]
    end

    subgraph Security ["Identity & Access Management"]
        AuthService["auth-service (:8081)<br/>JJWT 512-bit HS512 / BCrypt"]
    end

    subgraph Operations ["Core Hotel Operations Microservices"]
        CustomerService["customer-service (:8082)<br/>Guest Profiles & KYC"]
        HotelService["hotel-service (:8083)<br/>Hotel Catalog & Dynamic Search"]
        RoomService["room-service (:8084)<br/>Rooms & @Version Lock"]
        BookingService["booking-service (:8085)<br/>Date Overlap & 15m Sweep"]
    end

    subgraph Dining ["Food & In-Room Dining"]
        FoodService["food-service (:8086)<br/>Menus & POST /batch"]
        RSM["room-service-mgmt (:8087)<br/>In-Room Orders & KOT Engine"]
    end

    subgraph Finance ["Billing & Supply Chain"]
        BillingService["billing-service (:8088)<br/>18% GST & Payment Processing"]
        InventoryService["inventory-service (:8089)<br/>Double-Entry Stock Ledger"]
        NotificationService["notification-service (:8090)<br/>Multi-Channel Kafka Consumer"]
    end

    subgraph Messaging ["Apache Kafka 3.7.0 (KRaft Mode :9092)"]
        TopicBooking["hms.booking.events (3 Partitions)"]
        TopicPayment["hms.payment.events (3 Partitions)"]
        TopicRSM["hms.roomservice.events (3 Partitions)"]
        TopicInv["hms.inventory.events (3 Partitions)"]
    end

    subgraph Storage ["PostgreSQL 16 Multi-Database & Redis 7"]
        DB_Auth[("hms_auth_db")]
        DB_Cust[("hms_customer_db")]
        DB_Hotel[("hms_hotel_db")]
        DB_Room[("hms_room_db")]
        DB_Booking[("hms_booking_db")]
        DB_Food[("hms_food_db")]
        DB_RSM[("hms_rsm_db")]
        DB_Bill[("hms_billing_db")]
        DB_Inv[("hms_inventory_db")]
        DB_Notif[("hms_notification_db")]
        RedisCache[("Redis 7 Cache<br/>hotel_cache")]
    end

    %% Edge traffic
    Browser -->|HTTP REST| Gateway
    Gateway -.->|lb:// Service Resolution| Eureka
    Gateway ==>|/api/v1/auth/**| AuthService
    Gateway ==>|/api/v1/customers/**| CustomerService
    Gateway ==>|/api/v1/hotels/**| HotelService
    Gateway ==>|/api/v1/rooms/**| RoomService
    Gateway ==>|/api/v1/bookings/**| BookingService
    Gateway ==>|/api/v1/food/**| FoodService
    Gateway ==>|/api/v1/room-service/**| RSM
    Gateway ==>|/api/v1/billing/**| BillingService
    Gateway ==>|/api/v1/inventory/**| InventoryService
    Gateway ==>|/api/v1/notifications/**| NotificationService

    %% Eureka registrations
    AuthService -.-> Eureka
    CustomerService -.-> Eureka
    HotelService -.-> Eureka
    RoomService -.-> Eureka
    BookingService -.-> Eureka
    FoodService -.-> Eureka
    RSM -.-> Eureka
    BillingService -.-> Eureka
    InventoryService -.-> Eureka
    NotificationService -.-> Eureka

    %% Inter-service Feign calls
    RoomService -->|Feign: HotelClient| HotelService
    FoodService -->|Feign: HotelClient| HotelService
    InventoryService -->|Feign: HotelClient| HotelService
    BookingService -->|Feign + Resilience4j| CustomerService
    BookingService -->|Feign + Resilience4j| RoomService
    RSM -->|Feign: BookingClient| BookingService
    RSM -->|Feign: POST /batch| FoodService
    BillingService -->|Feign + Resilience4j| BookingService

    %% Kafka Events
    BookingService -->|Publishes| TopicBooking
    BillingService -->|Publishes| TopicPayment
    RSM -->|Publishes| TopicRSM
    InventoryService -->|Publishes| TopicInv

    TopicPayment -->|Auto-Confirm| BookingService
    TopicRSM -->|Auto-Deduct Stock| InventoryService
    TopicBooking --> NotificationService
    TopicPayment --> NotificationService
    TopicRSM --> NotificationService
    TopicInv --> NotificationService

    %% Databases
    AuthService --> DB_Auth
    CustomerService --> DB_Cust
    HotelService --> DB_Hotel
    HotelService --> RedisCache
    RoomService --> DB_Room
    BookingService --> DB_Booking
    FoodService --> DB_Food
    RSM --> DB_RSM
    BillingService --> DB_Bill
    InventoryService --> DB_Inv
    NotificationService --> DB_Notif
```

---

# 39. One-Page Interview Cheat Sheet

```text
========================================================================================
GRAND LUXE HOSPITALITY MANAGEMENT SYSTEM — QUICK INTERVIEW CHEAT SHEET
========================================================================================
Project Domain   : Enterprise Luxury Hotel Reservation & Operations Platform
Architecture     : Microservices with Database-per-Service & Event-Driven Backbone
Java Version     : Java 21 (LTS) — Records, Virtual Threads, Pattern Matching
Spring Boot      : 3.3.4 (Spring Cloud 2023.0.3 Leyton)
Microservices    : 12 Backend Java Services + 1 React 18 Frontend Portal
API Gateway      : Spring Cloud Gateway (:8080) on Netty WebFlux (Perimeter JWT Auth)
Service Registry : Spring Cloud Netflix Eureka Server (:8761) + Client-Side LoadBalancer
Databases        : PostgreSQL 16 (10 Isolated Databases hms_*_db) + HikariCP Pools
Caching Layer    : Redis 7 (Cache-Aside pattern in hotel-service, volatile-lru TTL)
Message Broker   : Apache Kafka 3.7.0 in KRaft Mode (3 Partitions, Key Hashing)
Security         : Perimeter JWT (512-bit HS512), BCrypt (12), Route-Level RBAC
Resilience       : Resilience4j 2.2.0 (CircuitBreaker + Retry with Exponential Backoff)
Testing          : 102 Automated Tests Passing (JUnit 5, Mockito, spring-kafka-test)
Deployment       : Docker Compose (PostgreSQL 16, Redis 7, Kafka KRaft 3.7.0)
========================================================================================
SERVICE PORTS & CORE RESPONSIBILITIES:
----------------------------------------------------------------------------------------
:8761 -> eureka-server           | Service registry & discovery server
:8080 -> api-gateway             | Netty reverse proxy, JWT validation, header injection
:8081 -> auth-service            | BCrypt hashing, user roles, 512-bit HS512 JWT issuance
:8082 -> customer-service        | Guest master records, address & KYC status
:8083 -> hotel-service           | Property catalog, search specifications, Redis cache
:8084 -> room-service            | Room inventory, base pricing, @Version optimistic locking
:8085 -> booking-service         | Date overlap conflict engine, 15m hold sweep worker
:8086 -> food-service            | Restaurant menu catalog, POST /batch lookup endpoint
:8087 -> room-service-mgmt       | In-room dining KOT state machine, price snapshots
:8088 -> billing-service         | 18% GST tax calculation, payment reconciliation
:8089 -> inventory-service       | Double-entry stock movement ledger, automated deductions
:8090 -> notification-service    | Multi-channel SMS/Email consumer listening to Kafka
:5173 -> hospitality-ui          | React 18 + Vite frontend portal
========================================================================================
COMMUNICATION PATTERNS:
----------------------------------------------------------------------------------------
OpenFeign + HC5  -> Synchronous operational validation (Booking -> Room/Customer, RSM -> Food)
Resilience4j     -> Resilient Delegate Wrappers (*Delegate.java) with @CircuitBreaker & @Retry
Kafka Topics     -> Asynchronous decoupled side-effects:
                    - hms.booking.events      (BookingCreated / Cancelled -> Notification)
                    - hms.payment.events      (PaymentCompleted -> Booking Confirm & Notif)
                    - hms.roomservice.events  (OrderDelivered -> Inventory Deduction)
                    - hms.inventory.events    (LowStockAlert -> Notification)
========================================================================================
CONCURRENCY & DATA CONSISTENCY:
----------------------------------------------------------------------------------------
Double-Booking   -> SQL: WHERE check_in < :newCheckOut AND check_out > :newCheckIn
Race Conditions  -> Hibernate @Version optimistic locking on Room & InventoryItem
Hold Expiration  -> Spring @Scheduled(cron = "0 */1 * * * *") sweeps unpaid holds > 15m
Price Freezing   -> Snapshots unitPrice and foodItemName inside room_service_order_items
========================================================================================
```

---

# 40. FINAL "DO NOT SAY" SECTION

To ensure total credibility in your interview, **do NOT claim any of the following**, as they are not present in this codebase:

| What NOT to Claim | Why You Must Avoid Claiming It | What You SHOULD Say Instead |
|---|---|---|
| **"We deployed on Kubernetes / EKS"** | *Not found in project.* There are no Kubernetes manifests (`k8s/*.yaml`) or Helm charts. | *"In our project, we containerized infrastructure using Docker Compose with dedicated health checks for PostgreSQL, Redis, and Kafka."* |
| **"We hosted on AWS (EC2, S3, RDS)"** | *Not found in project.* The project runs on local containerized infrastructure. | *"Our microservices run as container-ready Spring Boot applications connected to PostgreSQL 16 and Kafka in KRaft mode."* |
| **"We used Spring Cloud Config Server"** | *Not found in project.* Configuration is managed via local `application.yml` files and environment variables. | *"We externalized configurations in modular `application.yml` files with environment variable overrides for container portability."* |
| **"We used ZooKeeper for Kafka"** | *False.* The project explicitly uses Kafka 3.7.0 in **KRaft (Kafka Raft)** mode. Claiming ZooKeeper proves you didn't check the config! | *"We use modern Apache Kafka in KRaft mode, which eliminates ZooKeeper and uses Kafka's built-in Raft consensus protocol."* |
| **"All inter-service communication goes through Kafka"** | *False.* Critical validation calls use **OpenFeign synchronous REST**. Kafka is used for side-effects. | *"We use OpenFeign with Resilience4j for immediate validation (like room checks), and Kafka for asynchronous side-effects (like payment receipts and inventory deductions)."* |
| **"We used Distributed Tracing with Zipkin/Sleuth"** | *Not found in project.* Sleuth and Zipkin dependencies are not included in the build. | *"We use SLF4J and Logback for structured logging across services, with Spring Boot Actuator for health and circuit breaker metrics."* |
| **"We used Redis in all microservices"** | *False.* Redis is used **exclusively in `hotel-service`** for catalog caching. | *"We targeted Redis specifically at `hotel-service` using the Cache-Aside pattern, because hotel catalog reads outnumber writes by 10 to 1."* |
| **"We used Flyway or Liquibase for DB migrations"**| *Not found in project.* The project relies on Hibernate `ddl-auto: update` and `init-databases.sh`. | *"We initialized isolated PostgreSQL databases using Docker init scripts, with Hibernate handling entity schema binding."* |
| **"I personally built the entire system single-handedly"** | Over-claiming reduces believability. | Speak to your strong contributions: *"My focus was on core microservice development, inter-service resilience using Resilience4j, Kafka event integration, and concurrency control."* |

---
*End of Master Project Interview Document.*
