package com.hospitality.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitality.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumersTest {

    @Mock
    private NotificationService notificationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BookingEventConsumer bookingEventConsumer;

    @InjectMocks
    private OperationsEventConsumer operationsEventConsumer;

    @Test
    @DisplayName("Booking consumer handles BookingExpiredEvent")
    void testConsumeBookingExpired() {
        String payload = """
            {
                "bookingId": 200,
                "bookingReference": "BK-EXP-001",
                "customerId": 5,
                "reason": "15-minute hold timeout"
            }
            """;

        bookingEventConsumer.consumeBookingEvent(payload);

        verify(notificationService, times(1))
                .handleBookingExpired(eq("BK-EXP-001"), eq(5L), contains("15-minute hold"));
    }

    @Test
    @DisplayName("Booking consumer handles PaymentCompletedEvent on payment topic")
    void testConsumePaymentCompleted() {
        String payload = """
            {
                "invoiceNumber": "INV-2026-9999",
                "bookingReference": "BK-CONF-777",
                "customerId": 5,
                "totalAmount": 413.00,
                "status": "SUCCESS"
            }
            """;

        bookingEventConsumer.consumeBillingEvent(payload);

        verify(notificationService, times(1))
                .handlePaymentProcessed(eq("INV-2026-9999"), eq("BK-CONF-777"), eq(5L), any(BigDecimal.class), eq("SUCCESS"));

    }

    @Test
    @DisplayName("Operations consumer handles RoomServiceRequestedEvent")
    void testConsumeRoomServiceRequested() {
        String payload = """
            {
                "requestId": 55,
                "bookingReference": "BK-CONF-777",
                "customerId": 5,
                "requestType": "DINING_ORDER"
            }
            """;

        operationsEventConsumer.consumeRoomServiceEvent(payload);

        verify(notificationService, times(1))
                .handleRoomServiceRequested(eq(55L), eq("BK-CONF-777"), eq(5L), eq("DINING_ORDER"));
    }

    @Test
    @DisplayName("Operations consumer handles InventoryLowStockEvent")
    void testConsumeInventoryLowStock() {
        String payload = """
            {
                "itemCode": "TOWEL-LUX",
                "itemName": "Luxury Bath Towel",
                "currentStock": 8,
                "reorderLevel": 15
            }
            """;

        operationsEventConsumer.consumeInventoryEvent(payload);

        verify(notificationService, times(1))
                .handleInventoryLowStock(eq("TOWEL-LUX"), eq("Luxury Bath Towel"), eq(8), eq(15));
    }
}
