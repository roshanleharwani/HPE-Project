package com.payments.clearing.service;

import com.payments.clearing.producer.PaymentClearedProducer;
import org.springframework.stereotype.Service;

@Service
public class ClearingService {

    private final PaymentClearedProducer producer;

    public ClearingService(PaymentClearedProducer producer) {
        this.producer = producer;
    }

    public void process(String message) {

        // IMPORTANT: simulate clearing logic (gateway/bank processing)
        System.out.println("Clearing processing: " + message);

        // IMPORTANT: forward to next stage
        producer.send(message);
    }
}
