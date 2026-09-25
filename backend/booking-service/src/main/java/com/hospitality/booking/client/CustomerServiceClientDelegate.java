package com.hospitality.booking.client;

import com.hospitality.booking.dto.ApiResponse;
import com.hospitality.booking.exception.BadRequestException;
import com.hospitality.booking.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resilient delegate wrapper for CustomerClient OpenFeign calls in booking-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerServiceClientDelegate {

    private final CustomerClient customerClient;

    @CircuitBreaker(name = "customerServiceCircuitBreaker", fallbackMethod = "validateCustomerFallback")
    @Retry(name = "customerServiceRetry")
    public boolean validateCustomerActive(Long customerId) {
        log.info("Validating customer active status for ID: {}", customerId);
        ApiResponse<Boolean> response = customerClient.validateCustomerActive(customerId);
        return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
    }

    public boolean validateCustomerFallback(Long customerId, CallNotPermittedException ex) {
        log.error("Circuit Breaker [customerServiceCircuitBreaker] is OPEN for customer ID: {}. Reason: {}", 
                customerId, ex.getMessage());
        throw new ServiceUnavailableException("Customer service is temporarily unavailable (circuit breaker is OPEN). Please retry shortly.");
    }

    public boolean validateCustomerFallback(Long customerId, Throwable ex) {
        log.error("Fallback triggered for customer validation ID {}: {}", customerId, ex.getMessage());
        if (ex instanceof BadRequestException) {
            throw (BadRequestException) ex;
        }
        throw new ServiceUnavailableException("Customer service is currently unreachable: " + ex.getMessage());
    }
}
