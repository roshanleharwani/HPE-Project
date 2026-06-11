package com.payments.gateway.service;

import com.payments.gateway.dto.PaymentInitiatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.kafka.topic.initiated}")
    private String topicName;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        // Fire-and-forget: KafkaTemplate.send() is already async, returns CompletableFuture.
        // Adding whenComplete avoids blocking the calling virtual thread while still logging failures.
        kafkaTemplate.send(topicName, event.getTransactionId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment event for txn={}: {}",
                                event.getTransactionId(), ex.getMessage());
                    }
                });
    }
}
