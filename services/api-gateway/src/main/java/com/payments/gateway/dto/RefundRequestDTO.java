package com.payments.gateway.dto;

import lombok.Data;

@Data
public class RefundRequestDTO {
    private String transactionId;
    private String reason;
}
