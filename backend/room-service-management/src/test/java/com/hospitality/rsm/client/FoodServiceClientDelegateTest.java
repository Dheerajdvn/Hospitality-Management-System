package com.hospitality.rsm.client;

import com.hospitality.rsm.dto.ApiResponse;
import com.hospitality.rsm.dto.BatchFoodLookupRequest;
import com.hospitality.rsm.dto.FoodItemDto;
import com.hospitality.rsm.exception.BadRequestException;
import com.hospitality.rsm.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceClientDelegateTest {

    @Mock
    private FoodServiceClient foodServiceClient;

    @InjectMocks
    private FoodServiceClientDelegate delegate;

    @Test
    @DisplayName("Should successfully fetch batch items when food-service is healthy")
    void testGetFoodItemsBatch_Success() {
        FoodItemDto item = FoodItemDto.builder()
                .id(1L)
                .name("Biryani")
                .price(new BigDecimal("450.00"))
                .isAvailable(true)
                .build();
        BatchFoodLookupRequest req = new BatchFoodLookupRequest(List.of(1L));

        when(foodServiceClient.getFoodItemsBatch(any())).thenReturn(ApiResponse.success("Success", List.of(item)));

        List<FoodItemDto> result = delegate.getFoodItemsBatch(req);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Biryani", result.get(0).getName());
        verify(foodServiceClient).getFoodItemsBatch(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if food-service returns failure")
    void testGetFoodItemsBatch_ErrorResponse() {
        BatchFoodLookupRequest req = new BatchFoodLookupRequest(List.of(1L));
        when(foodServiceClient.getFoodItemsBatch(any())).thenReturn(ApiResponse.error("Failed"));

        assertThrows(BadRequestException.class, () -> delegate.getFoodItemsBatch(req));
    }

    @Test
    @DisplayName("Should execute circuit breaker OPEN fallback and throw ServiceUnavailableException (HTTP 503)")
    void testGetFoodItemsBatchFallback_CallNotPermitted() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("foodServiceCircuitBreaker");
        CallNotPermittedException openEx = CallNotPermittedException.createCallNotPermittedException(cb);
        BatchFoodLookupRequest req = new BatchFoodLookupRequest(List.of(1L));

        ServiceUnavailableException thrown = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.getFoodItemsBatchFallback(req, openEx)
        );

        assertTrue(thrown.getMessage().contains("circuit breaker is OPEN"));
    }

    @Test
    @DisplayName("Should execute generic fallback on connection error")
    void testGetFoodItemsBatchFallback_ConnectionError() {
        RuntimeException downstreamError = new RuntimeException("SocketTimeoutException :8086");
        BatchFoodLookupRequest req = new BatchFoodLookupRequest(List.of(1L));

        ServiceUnavailableException thrown = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.getFoodItemsBatchFallback(req, downstreamError)
        );

        assertTrue(thrown.getMessage().contains("unreachable"));
    }
}
