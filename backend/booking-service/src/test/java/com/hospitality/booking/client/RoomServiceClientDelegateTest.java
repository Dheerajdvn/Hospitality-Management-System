package com.hospitality.booking.client;

import com.hospitality.booking.dto.ApiResponse;
import com.hospitality.booking.dto.RoomDetailResponse;
import com.hospitality.booking.dto.RoomStatusUpdateDto;
import com.hospitality.booking.exception.BadRequestException;
import com.hospitality.booking.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceClientDelegateTest {

    @Mock
    private RoomClient roomClient;

    @InjectMocks
    private RoomServiceClientDelegate delegate;

    @Test
    @DisplayName("Should successfully get room by ID when room-service is healthy")
    void testGetRoomById_Success() {
        RoomDetailResponse mockRoom = RoomDetailResponse.builder()
                .id(1L)
                .roomNumber("101")
                .basePrice(new BigDecimal("4000.00"))
                .build();
        when(roomClient.getRoomById(1L)).thenReturn(ApiResponse.success("Success", mockRoom));

        RoomDetailResponse result = delegate.getRoomById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("101", result.getRoomNumber());
        verify(roomClient).getRoomById(1L);
    }

    @Test
    @DisplayName("Should throw BadRequestException if room not found")
    void testGetRoomById_NotFound_ThrowsBadRequest() {
        when(roomClient.getRoomById(999L)).thenReturn(ApiResponse.error("Not found"));

        assertThrows(BadRequestException.class, () -> delegate.getRoomById(999L));
    }

    @Test
    @DisplayName("Should execute circuit breaker OPEN fallback and throw ServiceUnavailableException (HTTP 503)")
    void testFetchRoomFallback_CallNotPermitted() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("roomServiceCircuitBreaker");
        CallNotPermittedException openEx = CallNotPermittedException.createCallNotPermittedException(cb);

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.fetchRoomFallback(1L, openEx)
        );

        assertTrue(ex.getMessage().contains("circuit breaker is OPEN"));
    }

    @Test
    @DisplayName("Should execute generic fallback when room-service connection fails")
    void testFetchRoomFallback_ConnectionError() {
        RuntimeException downstreamError = new RuntimeException("Connection refused :8084");

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.fetchRoomFallback(1L, downstreamError)
        );

        assertTrue(ex.getMessage().contains("unreachable"));
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException when status update circuit breaker is OPEN")
    void testUpdateRoomStatusFallback_CallNotPermitted() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("roomServiceCircuitBreaker");
        CallNotPermittedException openEx = CallNotPermittedException.createCallNotPermittedException(cb);
        RoomStatusUpdateDto dto = RoomStatusUpdateDto.builder().newStatus("BOOKED").build();

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.updateRoomStatusFallback(1L, dto, openEx)
        );

        assertTrue(ex.getMessage().contains("circuit breaker is OPEN"));
    }
}
