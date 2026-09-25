package com.hospitality.inventory.publisher;

import com.hospitality.inventory.event.InventoryLowStockEvent;
import com.hospitality.inventory.event.InventoryUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class KafkaInventoryEventPublisher implements InventoryEventPublisher {

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;
    private final boolean kafkaEnabled;
    private final String inventoryEventsTopic;

    @Autowired
    public KafkaInventoryEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${app.kafka.topics.inventory-events:hms.inventory.events}") String inventoryEventsTopic) {
        this.kafkaTemplate = Optional.ofNullable(kafkaTemplate);
        this.kafkaEnabled = kafkaEnabled;
        this.inventoryEventsTopic = inventoryEventsTopic;
    }

    @Override
    public void publishInventoryUpdated(InventoryUpdatedEvent event) {
        publishEvent("InventoryUpdatedEvent", event.getItemCode(), event);
    }

    @Override
    public void publishInventoryLowStock(InventoryLowStockEvent event) {
        publishEvent("InventoryLowStockEvent", event.getItemCode(), event);
    }

    private void publishEvent(String eventType, String key, Object payload) {
        if (kafkaEnabled && kafkaTemplate.isPresent()) {
            try {
                log.info("Publishing {} to Kafka topic '{}' with key '{}'", eventType, inventoryEventsTopic, key);
                kafkaTemplate.get().send(inventoryEventsTopic, key, payload)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Successfully delivered {} for item {} to partition {} at offset {}",
                                        eventType, key,
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            } else {
                                log.error("Failed to publish {} to Kafka: {}", eventType, ex.getMessage());
                            }
                        });
            } catch (Exception ex) {
                log.error("Exception publishing {} to Kafka: {}", eventType, ex.getMessage());
            }
        } else {
            log.info("[SIMULATED KAFKA BROKER] Topic: '{}' | Key: '{}' | EventType: {} | Payload: {}",
                    inventoryEventsTopic, key, eventType, payload);
        }
    }
}
