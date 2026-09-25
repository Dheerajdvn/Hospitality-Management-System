package com.hospitality.booking.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitality.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous Kafka consumer for payment lifecycle events on 'hms.payment.events'.
 * Drives the booking state machine upon receiving PaymentCompletedEvent or PaymentFailedEvent.
 * Fully idempotent to prevent duplicate confirmations on retried deliveries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class PaymentEventConsumer {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-events:hms.payment.events}",
            groupId = "${spring.kafka.consumer.group-id:hms-booking-group}"
    )
    public void consumePaymentEvent(String eventPayload) {
        log.info("Received payment event from Kafka: {}", eventPayload);
        try {
            JsonNode root = objectMapper.readTree(eventPayload);
            Long bookingId = root.path("bookingId").asLong();
            String bookingRef = root.path("bookingReference").asText();
            String status = root.path("status").asText();

            if (bookingId == null || bookingId == 0) {
                log.warn("Payment event missing bookingId, skipping: {}", eventPayload);
                return;
            }

            if ("SUCCESS".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
                log.info("Processing PaymentCompletedEvent for booking ID: {} (ref: {})", bookingId, bookingRef);
                bookingService.confirmBookingPayment(bookingId);
                log.info("Booking ID {} transitioned to CONFIRMED via asynchronous event", bookingId);
            } else if ("FAILED".equalsIgnoreCase(status) || "DECLINED".equalsIgnoreCase(status)) {
                String failureReason = root.path("failureReason").asText("Simulated payment transaction failed");
                log.warn("Processing PaymentFailedEvent for booking ID: {}. Reason: {}", bookingId, failureReason);
                bookingService.cancelBooking(bookingId, "Payment failed: " + failureReason);
            } else {
                log.info("Unhandled payment status '{}' for booking ID: {}", status, bookingId);
            }
        } catch (Exception ex) {
            log.error("Failed to parse and process payment event payload: {}", ex.getMessage(), ex);
        }
    }
}
