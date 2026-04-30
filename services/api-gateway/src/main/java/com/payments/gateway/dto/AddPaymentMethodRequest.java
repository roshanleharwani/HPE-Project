package com.payments.gateway.dto;

import lombok.Data;

@Data
public class AddPaymentMethodRequest {
    private String type;
    private String name;
    private String lastFour;
}
