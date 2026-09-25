package com.hospitality.rsm.client;

import com.hospitality.rsm.dto.ApiResponse;
import com.hospitality.rsm.dto.BatchFoodLookupRequest;
import com.hospitality.rsm.dto.FoodItemDto;
import com.hospitality.rsm.exception.BadRequestException;
import com.hospitality.rsm.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resilient delegate wrapper for FoodServiceClient OpenFeign calls in room-service-management.
 * Enforces Resilience4j Circuit Breaker and Retry semantics with fallback methods.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FoodServiceClientDelegate {

    private final FoodServiceClient foodServiceClient;

    @CircuitBreaker(name = "foodServiceCircuitBreaker", fallbackMethod = "getFoodItemsBatchFallback")
    @Retry(name = "foodServiceRetry")
    public List<FoodItemDto> getFoodItemsBatch(BatchFoodLookupRequest request) {
        log.info("Calling food-service batch lookup for {} items", request.getFoodItemIds() != null ? request.getFoodItemIds().size() : 0);
        ApiResponse<List<FoodItemDto>> response = foodServiceClient.getFoodItemsBatch(request);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BadRequestException("Failed to retrieve food items from food-service");
        }
        return response.getData();
    }

    public List<FoodItemDto> getFoodItemsBatchFallback(BatchFoodLookupRequest request, CallNotPermittedException ex) {
        log.error("Circuit Breaker [foodServiceCircuitBreaker] is OPEN. Call not permitted: {}", ex.getMessage());
        throw new ServiceUnavailableException("Food service is temporarily unavailable (circuit breaker is OPEN). Dining orders paused.");
    }

    public List<FoodItemDto> getFoodItemsBatchFallback(BatchFoodLookupRequest request, Throwable ex) {
        log.error("Fallback triggered for food batch lookup: {}", ex.getMessage());
        if (ex instanceof BadRequestException) {
            throw (BadRequestException) ex;
        }
        throw new ServiceUnavailableException("Food service is currently unreachable: " + ex.getMessage());
    }
}
