package com.hospitality.billing.client;

import com.hospitality.billing.dto.ApiResponse;
import com.hospitality.billing.dto.BookingDetailResponse;
import com.hospitality.billing.exception.BadRequestException;
import com.hospitality.billing.exception.ServiceUnavailableException;
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
class BookingServiceClientDelegateTest {

    @Mock
    private BookingClient bookingClient;

    @InjectMocks
    private BookingServiceClientDelegate delegate;

    @Test
    @DisplayName("Should successfully fetch booking data when booking-service responds normally")
    void testFetchBooking_Success() {
        BookingDetailResponse mockBooking = BookingDetailResponse.builder()
                .id(1L)
                .bookingReference("HMS-BK-12345")
                .totalAmount(new BigDecimal("10000.00"))
                .build();
        when(bookingClient.getBookingById(1L)).thenReturn(ApiResponse.success("Success", mockBooking));

        BookingDetailResponse result = delegate.fetchBooking(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("HMS-BK-12345", result.getBookingReference());
        verify(bookingClient).getBookingById(1L);
    }

    @Test
    @DisplayName("Should throw BadRequestException if booking-service returns not found")
    void testFetchBooking_NotFound_ThrowsBadRequest() {
        when(bookingClient.getBookingById(999L)).thenReturn(ApiResponse.error("Not found"));

        assertThrows(BadRequestException.class, () -> delegate.fetchBooking(999L));
    }

    @Test
    @DisplayName("Should execute circuit breaker OPEN fallback and throw ServiceUnavailableException (HTTP 503)")
    void testFetchBookingFallback_CallNotPermitted() {
        CircuitBreaker realCircuitBreaker = CircuitBreaker.ofDefaults("bookingServiceCircuitBreaker");
        CallNotPermittedException openException = CallNotPermittedException.createCallNotPermittedException(realCircuitBreaker);

        ServiceUnavailableException thrown = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.fetchBookingFallback(1L, openException)
        );

        assertTrue(thrown.getMessage().contains("circuit breaker is OPEN"));
    }

    @Test
    @DisplayName("Should execute generic fallback when downstream errors occur")
    void testFetchBookingFallback_GenericError() {
        RuntimeException downstreamError = new RuntimeException("Connection timed out to :8085");

        ServiceUnavailableException thrown = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.fetchBookingFallback(1L, downstreamError)
        );

        assertTrue(thrown.getMessage().contains("unreachable"));
    }

    @Test
    @DisplayName("Should gracefully handle confirmation fallback without propagating fatal exception")
    void testConfirmPaymentFallback() {
        CircuitBreaker realCircuitBreaker = CircuitBreaker.ofDefaults("bookingServiceCircuitBreaker");
        CallNotPermittedException openException = CallNotPermittedException.createCallNotPermittedException(realCircuitBreaker);

        // Fallback should handle quietly and log without throwing
        assertDoesNotThrow(() -> delegate.confirmPaymentFallback(1L, openException));
    }
}
