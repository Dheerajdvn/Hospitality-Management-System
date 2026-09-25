package com.hospitality.rsm.publisher;

import com.hospitality.rsm.event.RoomServiceRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class KafkaRoomServiceEventPublisher implements RoomServiceEventPublisher {

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;
    private final boolean kafkaEnabled;
    private final String roomServiceTopic;

    @Autowired
    public KafkaRoomServiceEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${app.kafka.topics.roomservice-events:hms.roomservice.events}") String roomServiceTopic) {
        this.kafkaTemplate = Optional.ofNullable(kafkaTemplate);
        this.kafkaEnabled = kafkaEnabled;
        this.roomServiceTopic = roomServiceTopic;
    }

    @Override
    public void publishRoomServiceRequested(RoomServiceRequestedEvent event) {
        String key = String.valueOf(event.getOrderId());
        if (kafkaEnabled && kafkaTemplate.isPresent()) {
            try {
                log.info("Publishing RoomServiceRequestedEvent to Kafka topic '{}' with key '{}'", roomServiceTopic, key);
                kafkaTemplate.get().send(roomServiceTopic, key, event)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Successfully delivered RoomServiceRequestedEvent for order ID {} to partition {} at offset {}",
                                        event.getOrderId(),
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            } else {
                                log.error("Failed to publish RoomServiceRequestedEvent to Kafka: {}", ex.getMessage());
                            }
                        });
            } catch (Exception ex) {
                log.error("Exception publishing RoomServiceRequestedEvent to Kafka: {}", ex.getMessage());
            }
        } else {
            log.info("[SIMULATED KAFKA BROKER] Topic: '{}' | Key: '{}' | EventType: RoomServiceRequestedEvent | Order: {}",
                    roomServiceTopic, key, event.getOrderNumber());
        }
    }
}
