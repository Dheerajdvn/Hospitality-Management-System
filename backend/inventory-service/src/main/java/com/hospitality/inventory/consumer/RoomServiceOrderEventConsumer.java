package com.hospitality.inventory.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitality.inventory.dto.StockMovementRequest;
import com.hospitality.inventory.entity.InventoryItem;
import com.hospitality.inventory.entity.StockMovementType;
import com.hospitality.inventory.repository.InventoryItemRepository;
import com.hospitality.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Asynchronous Kafka consumer in inventory-service listening to 'hms.roomservice.events'.
 * Decrements stock for consumed items and detects low stock thresholds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class RoomServiceOrderEventConsumer {

    private final InventoryService inventoryService;
    private final InventoryItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.roomservice-events:hms.roomservice.events}",
            groupId = "${spring.kafka.consumer.group-id:hms-inventory-group}"
    )
    public void consumeRoomServiceEvent(String eventPayload) {
        log.info("Received room service event in inventory-service: {}", eventPayload);
        try {
            JsonNode root = objectMapper.readTree(eventPayload);
            String orderNumber = root.path("orderNumber").asText();
            Long hotelId = root.path("hotelId").asLong();
            JsonNode itemsNode = root.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    String foodItemName = itemNode.path("foodItemName").asText();
                    int quantity = itemNode.path("quantity").asInt(1);

                    // Look up inventory item strictly scoped to the ordering hotel (with fallback)
                    List<InventoryItem> candidateItems = null;
                    if (hotelId != null && hotelId > 0) {
                        candidateItems = itemRepository.findByHotelId(hotelId);
                    }
                    if (candidateItems == null || candidateItems.isEmpty()) {
                        candidateItems = itemRepository.findAll();
                    }
                    if (candidateItems == null) {
                        candidateItems = List.of();
                    }

                    Optional<InventoryItem> match = candidateItems.stream()
                            .filter(i -> i.getName().equalsIgnoreCase(foodItemName) ||
                                         i.getName().toLowerCase().contains(foodItemName.toLowerCase()) ||
                                         foodItemName.toLowerCase().contains(i.getName().toLowerCase()))
                            .findFirst();

                    if (match.isPresent()) {
                        InventoryItem item = match.get();
                        log.info("Deducting inventory for item '{}' (Code: {}) by {} for order {}",
                                item.getName(), item.getItemCode(), quantity, orderNumber);

                        inventoryService.updateStock(item.getId(), StockMovementRequest.builder()
                                .movementType(StockMovementType.STOCK_OUT)
                                .quantity(quantity)
                                .reason("In-room dining order: " + orderNumber)
                                .performedBy("ROOM_SERVICE_SYSTEM")
                                .build());
                    } else {
                        log.debug("No direct inventory stock match found for food item '{}' (non-tracked ingredient)", foodItemName);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process room service event in inventory consumer: {}", ex.getMessage());
        }
    }
}

