# Java Backend Developer Interview Playbook (3–4+ Years Experience)
## Hospitality Management System — Microservices Project

---

## Part 1: Microservices Architecture & System Design

### Q1: "Walk me through the architecture of your Hospitality Management System. What are the key services and how do they interact?"
> **Answer**:
> *"Our system is an enterprise microservices platform built on Spring Boot 3.3.4 and Java 21, adhering to the Database-per-Service pattern with 10 dedicated PostgreSQL databases.
>
> At the edge sits the **Spring Cloud API Gateway (:8080)** running on Netty/WebFlux. It acts as the single reverse proxy, enforcing CORS and edge JWT authentication.
>
> Downstream, we separate core business domains:
> - **auth-service (:8081)**: Manages RBAC and signs 512-bit HS512 JWTs.
> - **customer-service (:8082)**: Manages guest records and KYC.
> - **hotel-service (:8083)**: Manages hotel catalogs with a Redis 7 Cache-Aside layer.
> - **room-service (:8084)**: Tracks room inventory using Hibernate `@Version` optimistic locking.
> - **booking-service (:8085)**: Implements mathematical date-overlap conflict queries and a 15-minute scheduled hold expiry engine.
> - **food-service (:8086)** & **room-service-management (:8087)**: Powers in-room dining with a multi-state KOT engine and batch resolution.
> - **billing-service (:8088)**: Computes 18% GST and handles payment reconciliation.
> - **inventory-service (:8089)**: Manages stock movement ledgers and low-stock reorder thresholds.
> - **notification-service (:8090)**: Handles multi-channel SMS and email notifications.
>
> Communication is split: **OpenFeign with Apache HttpClient 5** for real-time synchronous queries, and **Kafka / asynchronous events** for decoupled side-effects like payment confirmations and email notifications."*

---

### Q2: "Why did you choose the Database-per-Service pattern instead of a Shared Database?"
> **Answer**:
> *"In a monolithic or shared-database architecture, separate teams end up creating cross-table foreign keys and joined queries (e.g., joining `bookings` directly with `hotels` or `food_items`). Over time, this leads to tight schema coupling, database connection pool exhaustion, and the inability to scale or deploy a single service independently.
>
> By enforcing Database-per-Service with PostgreSQL:
> 1. Each microservice completely encapsulates its domain schema.
> 2. Schema migrations (`ALTER TABLE`) in `food-service` cannot break `billing-service`.
> 3. Each service can be scaled independently or backed up without taking down other domains."*

---

### Q3: "How do you handle distributed transactions across services without 2-Phase Commit (2PC)?"
> **Answer**:
> *"Two-phase commit (2PC) is a blocking protocol that severely damages availability and scalability in cloud microservices (violating the CAP theorem).
>
> Instead, we use the **Saga Pattern (Choreography/Orchestration)**:
> 1. When a guest creates a booking, `booking-service` sets status to `PENDING_PAYMENT` with a 15-minute temporary hold.
> 2. The client proceeds to `billing-service` to process payment.
> 3. Upon successful charge, a `PaymentProcessedEvent` is published.
> 4. `booking-service` consumes this event (or receives an idempotent Feign confirmation) and transitions the booking to `CONFIRMED`.
> 5. If payment fails or 15 minutes expire, a compensating transaction executes: the hold is released, marking the booking `CANCELLED`, freeing the room back to available inventory."*

---

## Part 2: Concurrency, Locking & Data Consistency

### Q4: "How did you prevent double-booking when multiple guests attempt to book the same room for overlapping dates simultaneously?"
> **Answer**:
> *"We implemented a two-tiered defense:
>
> 1. **Mathematical Interval Overlap Query**:
>    In `BookingRepository`, we query active bookings (`CONFIRMED` or `PENDING_PAYMENT`) using the condition:
>    ```sql
>    WHERE room_id = :roomId 
>      AND status IN ('CONFIRMED', 'PENDING_PAYMENT')
>      AND check_in < :newCheckOut 
>      AND check_out > :newCheckIn
>    ```
>    If any rows return, an immediate `HTTP 409 Conflict` is returned.
>
> 2. **Optimistic Locking (`@Version`)**:
>    The `Room` entity in `room-service` contains an `@Version Long version` column. If two concurrent transactions attempt to book or alter the room's status at the same millisecond, the second transaction fails with an `ObjectOptimisticLockingFailureException` upon commit, ensuring database consistency."*

---

### Q5: "What is the difference between Optimistic and Pessimistic Locking? Why did you pick Optimistic Locking here?"
> **Answer**:
> *"**Pessimistic Locking** (`SELECT FOR UPDATE`) places a row-level lock on the database record from the time it is read until the transaction finishes. This blocks all other concurrent readers/writers, which degrades throughput and introduces deadlock risks in web applications.
>
> **Optimistic Locking** assumes conflicts are infrequent. It uses a `@Version` counter. No database lock is held while the user is filling out their booking form. When the update is committed:
> ```sql
> UPDATE rooms SET status = 'BOOKED', version = 2 WHERE id = 1 AND version = 1;
> ```
> If another transaction already committed and bumped the version to 2, the row count is 0, causing Hibernate to roll back and throw `OptimisticLockException`. For hotel bookings and room dining, Optimistic Locking provides far superior read throughput and zero database deadlock overhead."*

---

### Q6: "How does the temporary 15-minute hold work in `booking-service`?"
> **Answer**:
> *"When a booking is initiated, its status is set to `PENDING_PAYMENT` and `createdAt` is timestamped. We configured a Spring `@Scheduled` background worker:
> ```java
> @Scheduled(cron = "0 */1 * * * *") // Runs every 60 seconds
> public void sweepExpiredBookings() { ... }
> ```
> The job runs a query:
> ```sql
> SELECT b FROM Booking b WHERE b.status = 'PENDING_PAYMENT' AND b.createdAt < :cutoffTime
> ```
> (where `cutoffTime = now() - 15 minutes`). All matching bookings are updated to `CANCELLED`, and an event is emitted so that room availability is automatically unlocked."*

---

## Part 3: High-Performance Caching & Redis

### Q7: "How is Redis used in the Hotel Service? Explain the Cache-Aside pattern."
> **Answer**:
> *"In hotel platforms, hotel catalog reads (browsing locations, amenities, photos) outnumber hotel profile writes by over 100:1.
>
> We implemented the **Cache-Aside Pattern** using Spring Data Redis and `@Cacheable(value = "hotel_cache", key = "#id")`:
> 1. When `GET /api/v1/hotels/{id}` is called, Spring checks Redis key `hotel_cache::1`.
> 2. **Cache Hit**: Returns serialized JSON in under 2ms without querying PostgreSQL.
> 3. **Cache Miss**: Reads PostgreSQL, writes the result to Redis with a TTL, and returns the response.
> 4. **Cache Invalidation**: When an administrator updates hotel details (`PUT /api/v1/hotels/{id}`), `@CacheEvict(value = "hotel_cache", key = "#id")` immediately evicts the stale key to prevent serving outdated pricing or contact details."*

---

### Q8: "How do you handle Cache Stampede (Thundering Herd) and Cache Penetration?"
> **Answer**:
> *- **Cache Penetration** (querying for IDs that do not exist in the database, hitting the DB every time): We configure Spring Cache to cache `null` values with a short TTL (e.g., 2 minutes) or validate IDs with a Bloom Filter.
> - **Cache Stampede** (a high-traffic key expires, causing thousands of concurrent requests to hit the database simultaneously): We use probabilistic early expiration or lock-based computation (Spring's `@Cacheable(sync = true)`), ensuring only one thread queries the database to repopulate the Redis key while other threads wait."*

---

## Part 4: API Gateway & Security Architecture

### Q9: "How does Spring Cloud Gateway differ from standard Spring MVC with Tomcat?"
> **Answer**:
> *"Standard Spring MVC uses the Servlet API running on Apache Tomcat. Each incoming HTTP request is assigned a dedicated thread from a thread pool. Under thousands of concurrent long-lived connections, thread stack memory overhead and context switching cause latency spikes.
>
> **Spring Cloud Gateway** is built on **Spring WebFlux and Project Reactor using Netty**. It operates an event-driven, non-blocking I/O event loop. A small number of worker threads can handle tens of thousands of concurrent connections asynchronously. Hence, `spring-boot-starter-web` must not be on the Gateway classpath."*

---

### Q10: "Explain your Edge Authentication architecture. How do downstream microservices trust the caller?"
> **Answer**:
> *"We apply the **Perimeter Edge Authentication Pattern**:
> 1. The Gateway's `JwtAuthenticationFilter` (`GlobalFilter`) intercepts every request at priority `-100`.
> 2. Whitelisted endpoints (`/api/v1/auth/**`, public `GET /hotels`, `GET /rooms`, `GET /food`, CORS `OPTIONS`) bypass the check.
> 3. For protected endpoints, the Gateway verifies the Bearer JWT's signature and expiration using JJWT.
> 4. If invalid or missing, the Gateway returns `HTTP 401 Unauthorized` immediately, shielding internal services from unauthorized load.
> 5. If valid, the Gateway unpacks the claims and mutates the reactive request headers, injecting:
>    - `X-User-Id`
>    - `X-User-Email`
>    - `X-User-Roles`
> Downstream microservices can trust these headers directly without re-verifying the JWT cryptographic signature on every internal call."*

---

## Part 5: Inter-Service Communication & OpenFeign

### Q11: "Why does Food Service expose a `POST /batch` endpoint instead of Room Service Management calling `GET /food/{id}` in a loop?"
> **Answer**:
> *"Calling `GET /food/{id}` in a loop is the **N+1 HTTP network anti-pattern**. If a guest orders 5 dishes, making 5 separate HTTP calls introduces network latency jitter, connection pool exhaustion, and cascading failure risks.
>
> By exposing `POST /api/v1/food/batch` with a body `{ foodItemIds: [1, 4, 6] }`, `food-service` executes a single optimized SQL query:
> ```sql
> SELECT * FROM food_items WHERE id IN (1, 4, 6);
> ```
> Downstream, `room-service-management` resolves all dish names, prices, and prep times in **a single network hop**."*

---

### Q12: "Why do we snapshot `unitPrice` and `foodItemName` in the `room_service_order_items` table?"
> **Answer**:
> *"To maintain **financial immutability and audit compliance**.
> If we only stored `food_item_id` and joined dynamically with `food_items` on invoice generation, any subsequent menu price change by a chef (e.g., Butter Chicken increasing from ₹520 to ₹600) would retroactively alter past receipts and billing totals. Storing a frozen price snapshot at order creation guarantees historical billing accuracy."*

---

## Part 6: Event-Driven Architecture & Kafka

### Q13: "What role does Apache Kafka play in the Hospitality Management System?"
> **Answer**:
> *"Kafka decouples non-blocking domain side-effects:
> 1. **Booking Lifecycle Events (`hms.booking.events`)**: Emitted on `BOOKING_CREATED`, `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`. The `notification-service` consumes these to send HTML email and SMS confirmations.
> 2. **Payment Events (`hms.payment.events`)**: Emitted on payment gateway success or decline.
>
> Using Kafka ensures that even if `notification-service` is temporarily down for maintenance, messages remain safely queued in Kafka partitions, and notifications are delivered once the service resumes without failing the user's booking flow."*

---

### Q14: "How did you ensure your services can run in environments where Kafka is not yet installed?"
> **Answer**:
> *"We implemented the **Resilient Event Publisher Pattern**:
> 1. We check if Kafka is active using Spring's `@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")`.
> 2. If Kafka is enabled, the publisher sends records via `KafkaTemplate`.
> 3. If Kafka is disabled (e.g., local dev or lightweight testing), the publisher writes structured event payloads to the application log with zero startup exceptions, allowing full local standalone execution."*

---

## Part 7: Spring Boot 3 & Java 21 Deep Dive

### Q15: "What are the major advantages of Java 21 and Spring Boot 3 in this architecture?"
> **Answer**:
> *"1. **Virtual Threads (Project Loom)**: Java 21 introduces lightweight virtual threads that eliminate carrier thread exhaustion during blocking I/O (like database or Feign calls).
> 2. **Jakarta EE 10 Namespace**: Spring Boot 3 migrated fully from `javax.*` to `jakarta.*` (Jakarta Persistence, Jakarta Validation).
> 3. **Record Types & Pattern Matching**: Java 21 records provide concise, immutable DTOs with built-in `equals()`, `hashCode()`, and `toString()`.
> 4. **Enhanced Spring Security 6**: Replaced deprecated `SecurityFilterChain` adapters with declarative lambda DSLs and stateless session management."*

---

### Q16: "How did you solve the Windows JVM 'Asia/Calcutta' PostgreSQL TimeZone bug?"
> **Answer**:
> *"On Windows machines running in the Indian Standard Time zone (`Asia/Calcutta`), the JVM passes the string `Asia/Calcutta` as the JDBC connection parameter `TimeZone`. PostgreSQL accepts `Asia/Kolkata` but rejects `Asia/Calcutta` with `PSQLException: invalid value for parameter TimeZone`.
>
> We resolved this permanently with two layers:
> 1. Setting `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` in a `static {}` block at the entry point of every `@SpringBootApplication`.
> 2. Adding `options: "-c timezone=UTC"` to HikariCP's `data-source-properties` in `application.yml`, ensuring deterministic UTC timestamps across all databases."*

---

## Part 8: Behavioral & Production Scenario Questions

### Q17: "A customer reports that their credit card was charged, but their hotel booking is still showing PENDING_PAYMENT. How do you troubleshoot and fix this in production?"
> **Answer**:
> *"Using the **STAR method**:
> - **Situation**: Distributed transaction discrepancy between Billing Service and Booking Service.
> - **Task**: Reconcile payment state, confirm the booking, and prevent customer distress.
> - **Action**:
>   1. Search `hms_billing_db.payments` using the transaction reference or customer email. Verify gateway status is `COMPLETED`.
>   2. Check Kafka lag and consumer logs on `hms.payment.events` to verify if the event was published and consumed.
>   3. If the event was dropped due to network timeout, call `billing-service`'s reconciliation endpoint or publish a retry event with the existing `payment_id`.
>   4. Ensure the endpoint is **idempotent** so re-processing does not double-charge or create duplicate invoices.
> - **Result**: The booking is immediately marked `CONFIRMED`, an automated confirmation SMS is sent, and an alert is added to our APM tool to monitor consumer lag."*

---

### Q18: "If the Hotel Service Redis cache goes down, does the entire application crash?"
> **Answer**:
> *"No. The Cache-Aside pattern provides graceful degradation. If Redis is unreachable, Spring Cache catches the connection exception, logs a warning, and falls back directly to the PostgreSQL database (`hms_hotel_db`). The application continues serving hotel catalog queries without downtime, albeit with slightly higher database query latency."*

---

## Part 9: Resilience4j, Circuit Breakers, Retries & Actuator Telemetry

### Q19: "What is a Circuit Breaker, why is it critical in microservices, and how does Resilience4j implement its state machine?"
> **Answer**:
> *"In a microservices architecture, synchronous inter-service calls (e.g., Billing Service calling Booking Service via OpenFeign) can fail or slow down. If downstream is unresponsive, incoming caller threads block waiting for timeouts, quickly exhausting the caller's Tomcat worker thread pool and causing **cascading system failure**.
>
> A **Circuit Breaker** acts as an electrical safety switch with three primary states:
> 1. **CLOSED**: Normal state. All requests flow downstream. Metrics (calls, errors, slow calls) are recorded in a sliding window.
> 2. **OPEN**: If the failure rate (e.g. >= 50%) or slow-call rate exceeds configured thresholds across the minimum number of calls, the circuit trips to OPEN. All subsequent calls immediately **fail fast** or execute a fallback method with `CallNotPermittedException`, sparing the downstream service and freeing Tomcat threads instantly.
> 3. **HALF_OPEN**: After a configured sleep duration (`waitDurationInOpenState`, e.g. 10s), the circuit transitions to HALF_OPEN to allow a limited probe of test requests (`permittedNumberOfCallsInHalfOpenState`, e.g. 3). If all pass, it transitions back to CLOSED; if failures persist, it returns to OPEN."*

---

### Q20: "Explain the difference between COUNT_BASED and TIME_BASED sliding windows in Resilience4j."
> **Answer**:
> *"Resilience4j calculates failure rates using one of two sliding window strategies:
> - **COUNT_BASED**: Evaluates the outcome of the last *N* requests (e.g., sliding window of 10 calls). Implemented as a ring buffer. Ideal for systems where request volume is steady or deterministic.
> - **TIME_BASED**: Evaluates requests over the last *N* seconds (e.g., sliding window of 60 seconds). Implemented as an array of time buckets. Useful for systems where request traffic spikes or drops significantly over time.
>
> In our Hospitality Management System, we chose a `COUNT_BASED` window of 10 calls with `minimum-number-of-calls: 5` and `failure-rate-threshold: 50%`, ensuring statistically sound decisions without triggering false trips on low traffic."*

---

### Q21: "Why did you separate OpenFeign calls into dedicated delegate components (e.g. BookingServiceClientDelegate) instead of placing @CircuitBreaker directly on the service or Feign interface?"
> **Answer**:
> *"This is a classic **Spring AOP dynamic proxy limitation**.
> 
> Spring AOP advice (`@CircuitBreaker`, `@Retry`, `@Transactional`, `@Async`) is applied via dynamic proxies that intercept external method invocations on Spring beans.
> 1. **Self-Invocation Gotcha**: If a method in `BillingServiceImpl` calls `this.fetchBooking()`, the call executes directly on the target object instance rather than through the Spring proxy. The `@CircuitBreaker` aspect advice is completely bypassed!
> 2. **Feign Interface Incompatibility**: Placing annotations on Feign client interfaces can cause bean proxy collision between Spring Cloud OpenFeign's proxy and Resilience4j's CGLIB proxy.
>
> By extracting inter-service calls into a dedicated Spring component (`BookingServiceClientDelegate`), we:
> - Guarantee that all invocations route through the Spring AOP proxy.
> - Provide clean, typed fallback methods (`fetchBookingFallback`) that return graceful degraded responses or HTTP 503 `ServiceUnavailableException`.
> - Make unit testing and mocking trivial without needing full Spring context initialization."*

---

### Q22: "How does @Retry with Exponential Backoff work, and what exceptions should be ignored vs retried?"
> **Answer**:
> *"The `@Retry` pattern automatically retries transient operations (e.g., brief network glitches, socket timeouts, temporary database lock contentions).
>
> We configure **Exponential Backoff**:
> - `max-attempts: 3`
> - `wait-duration: 500ms`
> - `enable-exponential-backoff: true`
> - `exponential-backoff-multiplier: 2` (attempts occur at 0ms, 500ms, and 1000ms).
>
> **Crucial Best Practice - Exception Filtering**:
> - **Retry Exceptions**: Only transient infrastructure errors like `feign.RetryableException`, `java.io.IOException`, and `TimeoutException`.
> - **Ignore Exceptions**: Business and validation exceptions like `BadRequestException` (HTTP 400) or `ResourceNotFoundException` (HTTP 404). Retrying an invalid client input will never succeed and merely wastes network bandwidth and CPU cycles!"*

---

### Q23: "How do you monitor Circuit Breakers and retries in production using Spring Boot Actuator?"
> **Answer**:
> *"We integrate `resilience4j-spring-boot3` and `resilience4j-micrometer` with Spring Boot Actuator:
> 1. `GET /actuator/health`: Exposes the component status for every registered circuit breaker (`state: "CLOSED"`, `failureRate`, `slowCalls`, `bufferedCalls`).
> 2. `GET /actuator/circuitbreakers`: Lists all circuit breaker instances and their runtime metrics.
> 3. `GET /actuator/circuitbreakerevents`: Emits chronological transition events (e.g. `ERROR`, `SUCCESS`, `STATE_TRANSITION` from CLOSED to OPEN).
> 4. `GET /actuator/retries`: Lists all active retry configurations.
> 5. `GET /actuator/metrics`: Micrometer metrics (`resilience4j.circuitbreaker.calls`, `resilience4j.circuitbreaker.state`) scraped by Prometheus and visualized on Grafana dashboards for production SRE monitoring."*
