package com.payments.gateway.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private String transactionId;
    private String userId;
    private String paymentMethodId;
    private BigDecimal amount;
    private String currency;
    private String description;
}
