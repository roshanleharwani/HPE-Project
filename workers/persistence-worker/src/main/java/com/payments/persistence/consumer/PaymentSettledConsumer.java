package com.payments.persistence.consumer;

import com.payments.persistence.service.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentSettledConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentSettledConsumer.class);
    private final PersistenceService persistenceService;

    public PaymentSettledConsumer(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @KafkaListener(topics = "payment_settled", groupId = "persistence-worker-group")
    public void consume(String message) {
        log.info("Received payment_settled event");
        try {
            persistenceService.persistPayment(message);
            log.info("Successfully persisted payment data");
        } catch (Exception e) {
            log.error("Failed to persist payment: {}", e.getMessage(), e);
            // In production: publish to DLQ for retry
        }
    }
}
