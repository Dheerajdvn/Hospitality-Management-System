package com.hospitality.booking.publisher;

import com.hospitality.booking.event.BookingCancelledEvent;
import com.hospitality.booking.event.BookingConfirmedEvent;
import com.hospitality.booking.event.BookingCreatedEvent;
import com.hospitality.booking.event.BookingExpiredEvent;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class KafkaBookingEventPublisher implements BookingEventPublisher {

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;
    private final boolean kafkaEnabled;
    private final String bookingEventsTopic;

    @Autowired
    public KafkaBookingEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${app.kafka.topics.booking-events:hms.booking.events}") String bookingEventsTopic) {
        this.kafkaTemplate = Optional.ofNullable(kafkaTemplate);
        this.kafkaEnabled = kafkaEnabled;
        this.bookingEventsTopic = bookingEventsTopic;
    }

    @Override
    public void publishBookingCreated(BookingCreatedEvent event) {
        publishEvent("BookingCreatedEvent", event.getBookingReference(), event);
    }

    @Override
    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        publishEvent("BookingConfirmedEvent", event.getBookingReference(), event);
    }

    @Override
    public void publishBookingCancelled(BookingCancelledEvent event) {
        publishEvent("BookingCancelledEvent", event.getBookingReference(), event);
    }

    @Override
    public void publishBookingExpired(BookingExpiredEvent event) {
        publishEvent("BookingExpiredEvent", event.getBookingReference(), event);
    }


    private void publishEvent(String eventType, String key, Object payload) {
        if (kafkaEnabled && kafkaTemplate.isPresent()) {
            try {
                log.info("Publishing {} to Kafka topic '{}' with key '{}'", eventType, bookingEventsTopic, key);
                kafkaTemplate.get().send(bookingEventsTopic, key, payload)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Successfully delivered {} to partition {} at offset {}",
                                        eventType,
                                        result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset());
                            } else {
                                log.error("Failed to publish {} to Kafka: {}", eventType, ex.getMessage());
                            }
                        });
            } catch (Exception ex) {
                log.error("Exception during Kafka publish for {}: {}", eventType, ex.getMessage());
            }
        } else {
            log.info("[SIMULATED KAFKA BROKER] Topic: '{}' | Key: '{}' | EventType: {} | Payload: {}",
                    bookingEventsTopic, key, eventType, payload);
        }
    }
}
