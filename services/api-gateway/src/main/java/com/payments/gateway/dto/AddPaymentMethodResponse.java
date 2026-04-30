package com.payments.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddPaymentMethodResponse {
    private boolean success;
    private String paymentMethodId;
}
