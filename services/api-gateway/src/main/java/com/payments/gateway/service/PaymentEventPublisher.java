package com.payments.gateway.service;

import com.payments.gateway.dto.PaymentInitiatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.kafka.topic.initiated}")
    private String topicName;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        kafkaTemplate.send(topicName, event.getTransactionId(), event);
    }
}
