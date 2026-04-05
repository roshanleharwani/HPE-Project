package com.payments.clearing.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentClearedProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentClearedProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String message) {

        // IMPORTANT: publish cleared event
        kafkaTemplate.send("payment_cleared", message);
    }
}
