package com.hospitality.billing.publisher;

import com.hospitality.billing.event.PaymentCompletedEvent;
import com.hospitality.billing.event.PaymentFailedEvent;

import com.hospitality.billing.event.PaymentProcessedEvent;
import com.hospitality.billing.event.RefundProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class KafkaBillingEventPublisher implements BillingEventPublisher {

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;
    private final boolean kafkaEnabled;
    private final String billingEventsTopic;
    private final String paymentEventsTopic;

    @Autowired
    public KafkaBillingEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${app.kafka.topics.billing-events:hms.billing.events}") String billingEventsTopic,
            @Value("${app.kafka.topics.payment-events:hms.payment.events}") String paymentEventsTopic) {
        this.kafkaTemplate = Optional.ofNullable(kafkaTemplate);
        this.kafkaEnabled = kafkaEnabled;
        this.billingEventsTopic = billingEventsTopic;
        this.paymentEventsTopic = paymentEventsTopic;
    }

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent("PaymentCompletedEvent", paymentEventsTopic, String.valueOf(event.getBookingId()), event);
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
        publishEvent("PaymentFailedEvent", paymentEventsTopic, String.valueOf(event.getBookingId()), event);
    }

    @Override
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        publishEvent("PaymentProcessedEvent", billingEventsTopic, event.getTransactionReference(), event);
    }

    @Override
    public void publishRefundProcessed(RefundProcessedEvent event) {
        publishEvent("RefundProcessedEvent", billingEventsTopic, event.getTransactionReference(), event);
    }

    private void publishEvent(String eventType, String topic, String key, Object payload) {
        if (kafkaEnabled && kafkaTemplate.isPresent()) {
            try {
                log.info("Publishing {} to Kafka topic '{}' with key '{}'", eventType, topic, key);
                kafkaTemplate.get().send(topic, key, payload)

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
                    billingEventsTopic, key, eventType, payload);
        }
    }
}
