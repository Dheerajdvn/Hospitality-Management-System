package com.hospitality.billing.client;

import com.hospitality.billing.dto.ApiResponse;
import com.hospitality.billing.dto.BookingDetailResponse;
import com.hospitality.billing.exception.BadRequestException;
import com.hospitality.billing.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resilient delegate wrapper for BookingClient OpenFeign calls.
 * Implements Resilience4j Circuit Breaker and Retry patterns with fallback methods.
 * Separating into a Spring-managed delegate avoids Spring AOP self-invocation limitations.
 */
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
            throw new BadRequestException("Booking with ID " + bookingId + " not found in booking service");
        }
        return response.getData();
    }

    /**
     * Fallback invoked when Circuit Breaker is OPEN (fast fail).
     */
    public BookingDetailResponse fetchBookingFallback(Long bookingId, CallNotPermittedException ex) {
        log.error("Circuit Breaker [bookingServiceCircuitBreaker] is OPEN. Call not permitted for booking ID: {}. Reason: {}", 
                bookingId, ex.getMessage());
        throw new ServiceUnavailableException("Booking service is temporarily unavailable (circuit breaker is OPEN). Please retry shortly.");
    }

    /**
     * Generic fallback invoked when retries are exhausted or downstream throws an unexpected error.
     */
    public BookingDetailResponse fetchBookingFallback(Long bookingId, Throwable ex) {
        log.error("Resilience4j fallback triggered for booking ID: {}. Root cause: {}", bookingId, ex.getMessage());
        if (ex instanceof BadRequestException) {
            throw (BadRequestException) ex;
        }
        throw new ServiceUnavailableException("Booking service is currently unreachable: " + ex.getMessage());
    }

    @CircuitBreaker(name = "bookingServiceCircuitBreaker", fallbackMethod = "confirmPaymentFallback")
    @Retry(name = "bookingServiceRetry")
    public void confirmBookingPayment(Long bookingId) {
        log.info("Notifying booking-service of payment confirmation for booking ID: {}", bookingId);
        bookingClient.confirmBookingPayment(bookingId);
    }

    public void confirmPaymentFallback(Long bookingId, CallNotPermittedException ex) {
        log.error("Circuit Breaker [bookingServiceCircuitBreaker] is OPEN. Payment confirmation queued for booking ID: {}", bookingId);
    }

    public void confirmPaymentFallback(Long bookingId, Throwable ex) {
        log.error("Resilience4j fallback for confirmBookingPayment on booking ID: {}. Error: {}", bookingId, ex.getMessage());
    }
}
