package com.payments.gateway.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentInitiatedEvent {
    private String paymentIntentId;
    private String transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime timestamp;
}
