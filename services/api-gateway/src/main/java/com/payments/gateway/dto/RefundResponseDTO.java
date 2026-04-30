package com.payments.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundResponseDTO {
    private boolean success;
    private String refundId;
    private String message;
}
