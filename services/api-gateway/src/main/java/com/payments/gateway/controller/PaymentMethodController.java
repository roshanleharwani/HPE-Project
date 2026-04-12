package com.payments.gateway.controller;

import com.payments.gateway.dto.PaymentMethodDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payment-methods")
public class PaymentMethodController {

    @PostMapping
    public ResponseEntity<PaymentMethodDTO> addPaymentMethod() {
        // Returning mock implementation for POST
        return ResponseEntity.ok(
            PaymentMethodDTO.builder()
                .id(UUID.randomUUID().toString())
                .methodType("CARD")
                .provider("VISA")
                .lastFour("4242")
                .build()
        );
    }

    @GetMapping
    public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethods() {
        // Returning mock implementation for GET
        return ResponseEntity.ok(List.of(
            PaymentMethodDTO.builder()
                .id(UUID.randomUUID().toString())
                .methodType("CARD")
                .provider("VISA")
                .lastFour("4242")
                .build()
        ));
    }
}
