package com.payments.clearing.consumer;

import com.payments.clearing.service.ClearingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentAuthorizedConsumer {

    private final ClearingService clearingService;

    public PaymentAuthorizedConsumer(ClearingService clearingService) {
        this.clearingService = clearingService;
    }

    @KafkaListener(topics = "payment_authorized", groupId = "clearing-group")
    public void consume(String message) {

        // IMPORTANT: receives authorized payment event
        clearingService.process(message);
    }
}
