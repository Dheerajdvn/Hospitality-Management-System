package com.hospitality.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitality.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer in notification-service for room-service operations and inventory warnings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class OperationsEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.roomservice-events:hms.roomservice.events}",
            groupId = "${spring.kafka.consumer.group-id:hms-notification-group}"
    )
    public void consumeRoomServiceEvent(String eventPayload) {
        log.info("Received room service event from Kafka: {}", eventPayload);
        try {
            JsonNode root = objectMapper.readTree(eventPayload);
            Long requestId = root.path("requestId").asLong();
            String bookingReference = root.path("bookingReference").asText();
            Long customerId = root.path("customerId").asLong();
            String serviceType = root.path("requestType").asText(root.path("orderType").asText("ROOM_SERVICE"));

            notificationService.handleRoomServiceRequested(requestId, bookingReference, customerId, serviceType);
        } catch (Exception ex) {
            log.error("Failed to parse and process room service event: {}", ex.getMessage());
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.inventory-events:hms.inventory.events}",
            groupId = "${spring.kafka.consumer.group-id:hms-notification-group}"
    )
    public void consumeInventoryEvent(String eventPayload) {
        log.info("Received inventory event from Kafka: {}", eventPayload);
        try {
            JsonNode root = objectMapper.readTree(eventPayload);
            if (root.has("currentStock") && root.has("reorderLevel")) {
                int currentStock = root.path("currentStock").asInt();
                int threshold = root.path("reorderLevel").asInt();
                if (currentStock <= threshold) {
                    String itemCode = root.path("itemCode").asText();
                    String itemName = root.path("itemName").asText(itemCode);
                    notificationService.handleInventoryLowStock(itemCode, itemName, currentStock, threshold);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to parse and process inventory event: {}", ex.getMessage());
        }
    }
}
