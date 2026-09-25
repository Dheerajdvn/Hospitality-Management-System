package com.hospitality.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitality.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class BookingEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.booking-events:hms.booking.events}", groupId = "${spring.kafka.consumer.group-id:hms-notification-group}")
    public void consumeBookingEvent(String eventPayload) {
        log.info("Received booking event from Kafka: {}", eventPayload);
        try {
            JsonNode node = objectMapper.readTree(eventPayload);
            String ref = node.path("bookingReference").asText();
            Long customerId = node.path("customerId").asLong();
            BigDecimal amount = new BigDecimal(node.path("totalAmount").asText("0.00"));

            if (node.has("holdExpiresAt") && !node.get("holdExpiresAt").isNull()) {
                // BookingCreatedEvent
                notificationService.handleBookingCreated(ref, customerId, amount, node.path("holdExpiresAt").asText());
            } else if (node.has("reason") && !node.get("reason").isNull()) {
                // BookingExpiredEvent
                notificationService.handleBookingExpired(ref, customerId, node.path("reason").asText());
            } else if (node.has("cancellationReason") && !node.get("cancellationReason").isNull()) {
                // BookingCancelledEvent
                notificationService.handleBookingCancelled(ref, customerId, node.path("cancellationReason").asText());
            } else {
                // BookingConfirmedEvent
                notificationService.handleBookingConfirmed(ref, customerId, amount);
            }
        } catch (Exception ex) {
            log.error("Failed to parse and process booking event payload: {}", ex.getMessage());
        }
    }

    @KafkaListener(topics = {
            "${app.kafka.topics.billing-events:hms.billing.events}",
            "${app.kafka.topics.payment-events:hms.payment.events}"
    }, groupId = "${spring.kafka.consumer.group-id:hms-notification-group}")
    public void consumeBillingEvent(String eventPayload) {
        log.info("Received billing/payment event from Kafka: {}", eventPayload);
        try {
            JsonNode node = objectMapper.readTree(eventPayload);
            String invoiceNumber = node.path("invoiceNumber").asText();
            String bookingReference = node.path("bookingReference").asText();
            Long customerId = node.path("customerId").asLong();

            if (node.has("refundAmount") && !node.get("refundAmount").isNull()) {
                // RefundProcessedEvent
                BigDecimal refundAmount = new BigDecimal(node.path("refundAmount").asText("0.00"));
                String reason = node.path("refundReason").asText("Cancellation refund");
                notificationService.handleRefundProcessed(invoiceNumber, bookingReference, customerId, refundAmount, reason);
            } else {
                // PaymentProcessedEvent / PaymentCompletedEvent / PaymentFailedEvent
                BigDecimal totalAmount = new BigDecimal(node.path("totalAmount").asText("0.00"));
                String status = node.path("status").asText("SUCCESS");
                notificationService.handlePaymentProcessed(invoiceNumber, bookingReference, customerId, totalAmount, status);
            }
        } catch (Exception ex) {
            log.error("Failed to parse and process billing event payload: {}", ex.getMessage());
        }
    }
}

