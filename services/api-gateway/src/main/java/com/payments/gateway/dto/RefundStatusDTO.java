package com.payments.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundStatusDTO {
    private String id;
    private String transactionId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private LocalDateTime requestDate;
    private LocalDateTime completedDate;
}
