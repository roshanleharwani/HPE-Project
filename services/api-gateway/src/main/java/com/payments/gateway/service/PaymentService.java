package com.payments.gateway.service;

import com.payments.gateway.dto.PaymentInitiatedEvent;
import com.payments.gateway.dto.PaymentRequest;
import com.payments.gateway.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IdempotencyService idempotencyService;
    private final PaymentEventPublisher eventPublisher;

    public PaymentResponse processPayment(PaymentRequest request) {
        // 1. Validate request (basic here)
        if (request.getTransactionId() == null || request.getAmount() == null) {
            throw new IllegalArgumentException("Invalid payment request");
        }

        // 2. Check Idempotency
        if (idempotencyService.isDuplicate(request.getTransactionId())) {
            return PaymentResponse.builder()
                    .transactionId(request.getTransactionId())
                    .status("REJECTED")
                    .message("Duplicate transaction detected.")
                    .build();
        }

        // 3. Create Payment Intent (Simulated DB generation)
        String paymentIntentId = UUID.randomUUID().toString();

        // 4. Publish Kafka Event
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .paymentIntentId(paymentIntentId)
                .transactionId(request.getTransactionId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("INITIATED")
                .timestamp(LocalDateTime.now())
                .build();
                
        eventPublisher.publishPaymentInitiated(event);

        // 5. Return success response
        return PaymentResponse.builder()
                .paymentIntentId(paymentIntentId)
                .transactionId(request.getTransactionId())
                .status("PROCESSING")
                .message("Payment initiated successfully.")
                .build();
    }
}
