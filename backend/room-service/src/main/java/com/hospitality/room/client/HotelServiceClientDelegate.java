package com.hospitality.room.client;

import com.hospitality.room.dto.ApiResponse;
import com.hospitality.room.exception.BadRequestException;
import com.hospitality.room.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resilient delegate wrapper for HotelClient OpenFeign calls in room-service.
 * Enforces Resilience4j Circuit Breaker and Retry semantics with fallback behavior.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelServiceClientDelegate {

    private final HotelClient hotelClient;

    @CircuitBreaker(name = "hotelServiceCircuitBreaker", fallbackMethod = "validateHotelFallback")
    @Retry(name = "hotelServiceRetry")
    public boolean validateHotelActive(Long hotelId) {
        log.info("Validating active status of hotel ID: {}", hotelId);
        ApiResponse<Boolean> response = hotelClient.validateHotelActive(hotelId);
        return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
    }

    public boolean validateHotelFallback(Long hotelId, CallNotPermittedException ex) {
        log.error("Circuit Breaker [hotelServiceCircuitBreaker] is OPEN for hotel ID: {}. Reason: {}", hotelId, ex.getMessage());
        throw new ServiceUnavailableException("Hotel service is temporarily unavailable (circuit breaker is OPEN). Please try again shortly.");
    }

    public boolean validateHotelFallback(Long hotelId, Throwable ex) {
        log.error("Fallback triggered for hotel ID {}: {}", hotelId, ex.getMessage());
        if (ex instanceof BadRequestException) {
            throw (BadRequestException) ex;
        }
        throw new ServiceUnavailableException("Hotel service is currently unreachable: " + ex.getMessage());
    }
}
