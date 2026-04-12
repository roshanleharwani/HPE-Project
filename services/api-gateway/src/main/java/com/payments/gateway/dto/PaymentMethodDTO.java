package com.payments.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentMethodDTO {
    private String id;
    private String methodType;
    private String provider;
    private String lastFour;
}
