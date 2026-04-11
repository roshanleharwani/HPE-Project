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

       
        System.out.println("Clearing processing: " + message);
        
        producer.send(message);
    }
}
