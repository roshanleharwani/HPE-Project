package com.payments.gateway.controller;

import com.payments.gateway.dto.TransactionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getTransactions() {
        // Returning mock implementation
        return ResponseEntity.ok(List.of(
            TransactionDTO.builder()
                .transactionId(UUID.randomUUID().toString())
                .paymentIntentId(UUID.randomUUID().toString())
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status("SETTLED")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable String id) {
        // Returning mock implementation
        return ResponseEntity.ok(
            TransactionDTO.builder()
                .transactionId(id)
                .paymentIntentId(UUID.randomUUID().toString())
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status("SETTLED")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build()
        );
    }
}
