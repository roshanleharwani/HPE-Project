package com.payments.gateway.controller;

import com.payments.gateway.dto.PaymentRequest;
import com.payments.gateway.dto.PaymentResponse;
import com.payments.gateway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.processPayment(request);
            if ("REJECTED".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response); // 409 Conflict for duplicate
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(PaymentResponse.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(PaymentResponse.builder()
                    .status("ERROR")
                    .message("Internal server error: " + e.getMessage())
                    .build());
        }
    }
}
