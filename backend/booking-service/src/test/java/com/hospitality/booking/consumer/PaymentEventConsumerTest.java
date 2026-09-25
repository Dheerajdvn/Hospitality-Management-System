package com.hospitality.booking.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitality.booking.dto.BookingResponse;
import com.hospitality.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private BookingService bookingService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    @DisplayName("Should confirm booking when payment event status is SUCCESS")
    void testConsumePaymentSuccess() {
        String payload = """
            {
                "bookingId": 101,
                "bookingReference": "BK-2026-ABC123",
                "customerId": 10,
                "totalAmount": 700.00,
                "status": "SUCCESS",
                "transactionReference": "TXN-998877"
            }
            """;

        when(bookingService.confirmBookingPayment(101L)).thenReturn(BookingResponse.builder().id(101L).build());

        paymentEventConsumer.consumePaymentEvent(payload);

        verify(bookingService, times(1)).confirmBookingPayment(101L);
        verify(bookingService, never()).cancelBooking(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should cancel booking and release hold when payment event status is FAILED")
    void testConsumePaymentFailed() {
        String payload = """
            {
                "bookingId": 102,
                "bookingReference": "BK-2026-FAIL12",
                "customerId": 10,
                "totalAmount": 700.00,
                "status": "FAILED",
                "failureReason": "Insufficient funds"
            }
            """;

        when(bookingService.cancelBooking(eq(102L), contains("Insufficient funds")))
                .thenReturn(BookingResponse.builder().id(102L).build());

        paymentEventConsumer.consumePaymentEvent(payload);

        verify(bookingService, times(1)).cancelBooking(eq(102L), contains("Insufficient funds"));
        verify(bookingService, never()).confirmBookingPayment(anyLong());
    }

    @Test
    @DisplayName("Should handle malformed JSON payload gracefully without throwing exception")
    void testConsumeMalformedPayload() {
        String invalidPayload = "{invalid-json";

        paymentEventConsumer.consumePaymentEvent(invalidPayload);

        verify(bookingService, never()).confirmBookingPayment(anyLong());
        verify(bookingService, never()).cancelBooking(anyLong(), anyString());
    }
}
