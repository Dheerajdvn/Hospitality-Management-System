package com.hospitality.room.client;

import com.hospitality.room.dto.ApiResponse;
import com.hospitality.room.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceClientDelegateTest {

    @Mock
    private HotelClient hotelClient;

    @InjectMocks
    private HotelServiceClientDelegate delegate;

    @Test
    @DisplayName("Should successfully validate hotel active when hotel-service is available")
    void testValidateHotelActive_Success() {
        when(hotelClient.validateHotelActive(1L)).thenReturn(ApiResponse.success("Success", true));

        boolean isValid = delegate.validateHotelActive(1L);

        assertTrue(isValid);
        verify(hotelClient).validateHotelActive(1L);
    }

    @Test
    @DisplayName("Should return false when hotel is inactive")
    void testValidateHotelActive_Inactive() {
        when(hotelClient.validateHotelActive(2L)).thenReturn(ApiResponse.success("Inactive", false));

        boolean isValid = delegate.validateHotelActive(2L);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should execute circuit breaker OPEN fallback and throw ServiceUnavailableException (HTTP 503)")
    void testValidateHotelFallback_CallNotPermitted() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("hotelServiceCircuitBreaker");
        CallNotPermittedException openEx = CallNotPermittedException.createCallNotPermittedException(cb);

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.validateHotelFallback(1L, openEx)
        );

        assertTrue(ex.getMessage().contains("circuit breaker is OPEN"));
    }

    @Test
    @DisplayName("Should execute generic fallback on connection error")
    void testValidateHotelFallback_ConnectionError() {
        RuntimeException downstreamError = new RuntimeException("Connection refused :8083");

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.validateHotelFallback(1L, downstreamError)
        );

        assertTrue(ex.getMessage().contains("unreachable"));
    }
}
