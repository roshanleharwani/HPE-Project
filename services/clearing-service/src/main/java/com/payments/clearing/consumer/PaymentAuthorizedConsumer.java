package com.payments.clearing.consumer;

import com.payments.clearing.service.ClearingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class PaymentAuthorizedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentAuthorizedConsumer.class);
    private final ClearingService clearingService;

    public PaymentAuthorizedConsumer(ClearingService clearingService) {
        this.clearingService = clearingService;
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        autoCreateTopics = "true"
    )
    @KafkaListener(topics = "payment_authorized", groupId = "clearing-group")
    public void consume(String message) {
        clearingService.process(message);
    }

    @DltHandler
    public void handleDlt(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Event from topic {} routed to DLT. Message: {}", topic, message);
    }
}
