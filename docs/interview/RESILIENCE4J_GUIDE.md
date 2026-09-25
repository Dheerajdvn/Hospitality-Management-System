# Resilience4j & Spring Boot Actuator Architecture Guide

## Overview

In the **Hospitality Management System (HMS)**, synchronous inter-service calls (OpenFeign) are protected against latency spikes, cascading failures, and transient network errors using **Resilience4j 2.2.0** integrated with **Spring Cloud CircuitBreaker**, **Spring Boot 3.3.4 Actuator**, and **Spring AOP**.

---

## 1. Protected Inter-Service Feign Call Paths

| Caller Service | Downstream Service | Protected Method | Circuit Breaker Name | Retry Policy Name |
| :--- | :--- | :--- | :--- | :--- |
| **billing-service** (:8088) | `booking-service` (:8085) | `fetchBooking()`, `confirmBookingPayment()` | `bookingServiceCircuitBreaker` | `bookingServiceRetry` |
| **booking-service** (:8085) | `room-service` (:8084) | `getRoomById()`, `updateRoomStatus()` | `roomServiceCircuitBreaker` | `roomServiceRetry` |
| **booking-service** (:8085) | `customer-service` (:8082) | `validateCustomerActive()` | `customerServiceCircuitBreaker` | `customerServiceRetry` |
| **room-service-management** (:8087) | `food-service` (:8086) | `getFoodItemsBatch()` | `foodServiceCircuitBreaker` | `foodServiceRetry` |
| **room-service** (:8084) | `hotel-service` (:8083) | `validateHotelActive()` | `hotelServiceCircuitBreaker` | `hotelServiceRetry` |

---

## 2. Architectural Design: Resilient Delegate Pattern

### Why Delegate Wrappers instead of Annotating Feign Interfaces?
1. **Spring AOP Dynamic Proxy Limitation (Self-Invocation)**:
   In Spring, AOP advice (`@CircuitBreaker`, `@Retry`, `@Transactional`) operates by intercepting calls via dynamic proxies. When method `A()` calls method `B()` in the same class via `this.b()`, the proxy is bypassed and the circuit breaker never trips.
2. **Feign Proxy Collision**:
   Directly annotating OpenFeign interface methods with Resilience4j can lead to proxy collisions between Spring Cloud OpenFeign's invocation handler and Spring AOP's CGLIB proxy.
3. **Clean Fallbacks**:
   Separating remote calls into a `@Component` delegate (e.g. `BookingServiceClientDelegate`) isolates resilience policies, allows typed fallback methods (e.g. handling `CallNotPermittedException` explicitly), and makes unit testing effortless.

### Example Delegate Implementation:
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingServiceClientDelegate {

    private final BookingClient bookingClient;

    @CircuitBreaker(name = "bookingServiceCircuitBreaker", fallbackMethod = "fetchBookingFallback")
    @Retry(name = "bookingServiceRetry")
    public BookingDetailResponse fetchBooking(Long bookingId) {
        log.info("Calling booking-service for booking ID: {}", bookingId);
        ApiResponse<BookingDetailResponse> response = bookingClient.getBookingById(bookingId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BadRequestException("Booking with ID " + bookingId + " not found");
        }
        return response.getData();
    }

    // Typed fallback when Circuit Breaker is OPEN (Fast-fail)
    public BookingDetailResponse fetchBookingFallback(Long bookingId, CallNotPermittedException ex) {
        log.error("Circuit Breaker is OPEN for booking-service. Fast-failing ID {}: {}", bookingId, ex.getMessage());
        throw new ServiceUnavailableException("Booking service is temporarily unavailable (circuit breaker is OPEN). Please retry shortly.");
    }

    // Fallback when retries are exhausted or downstream connection times out
    public BookingDetailResponse fetchBookingFallback(Long bookingId, Throwable ex) {
        log.error("Resilience4j fallback triggered for booking ID {}: {}", bookingId, ex.getMessage());
        if (ex instanceof BadRequestException) {
            throw (BadRequestException) ex;
        }
        throw new ServiceUnavailableException("Booking service is currently unreachable: " + ex.getMessage());
    }
}
```

---

## 3. Resilience4j Configuration in `application.yml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      bookingServiceCircuitBreaker:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10000ms
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        register-health-indicator: true
        record-exceptions:
          - feign.FeignException
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignore-exceptions:
          - com.hospitality.billing.exception.BadRequestException
          - com.hospitality.billing.exception.ResourceNotFoundException
  retry:
    instances:
      bookingServiceRetry:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - feign.RetryableException
          - java.io.IOException
        ignore-exceptions:
          - com.hospitality.billing.exception.BadRequestException
          - com.hospitality.billing.exception.ResourceNotFoundException
```

---

## 4. Actuator Endpoints & Monitoring

| Endpoint | Method | Purpose |
| :--- | :--- | :--- |
| `/actuator/health` | GET | Shows aggregated health, including circuit breaker states (`CLOSED`, `OPEN`, `HALF_OPEN`), failure rate, and call statistics. |
| `/actuator/circuitbreakers` | GET | Detailed live metrics per circuit breaker instance. |
| `/actuator/circuitbreakerevents` | GET | Audit log of circuit breaker events (`ERROR`, `SUCCESS`, `STATE_TRANSITION`). |
| `/actuator/retries` | GET | Configured retry instances. |
| `/actuator/retryevents` | GET | Audit log of retry attempts and outcomes. |
| `/actuator/metrics` | GET | Prometheus/Micrometer metrics (`resilience4j.circuitbreaker.calls`, etc.). |

---

## 5. Live Actuator Response Verified

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "bookingServiceCircuitBreaker": {
          "status": "UP",
          "details": {
            "failureRate": "-1.0%",
            "failureRateThreshold": "50.0%",
            "slowCallRate": "-1.0%",
            "slowCallRateThreshold": "100.0%",
            "bufferedCalls": 0,
            "slowCalls": 0,
            "slowFailedCalls": 0,
            "failedCalls": 0,
            "notPermittedCalls": 0,
            "state": "CLOSED"
          }
        }
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```
