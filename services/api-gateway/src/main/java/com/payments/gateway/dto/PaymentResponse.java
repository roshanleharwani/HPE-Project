package com.payments.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String paymentIntentId;
    private String transactionId;
    private String status;
    private String message;
}
