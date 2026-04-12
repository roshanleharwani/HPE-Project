package com.payments.gateway.kafka;

import com.payments.gateway.dto.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.kafka.topic.initiated:payment_initiated}")
    private String topicName;

    public void sendPaymentInitiatedEvent(PaymentInitiatedEvent event) {
        log.info("Sending payment initiated event to topic {}: {}", topicName, event);
        kafkaTemplate.send(topicName, event.getTransactionId(), event);
    }
}
