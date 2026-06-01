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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    // Limit to 2000 concurrent processing requests for graceful degradation
    private final Semaphore rateLimiter = new Semaphore(2000);

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        if (!rateLimiter.tryAcquire()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(PaymentResponse.builder()
                    .status("FAILED")
                    .message("Too Many Requests. System under heavy load.")
                    .build());
        }
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
            e.printStackTrace();
            System.err.println("API GATEWAY ERROR: " + e.getClass().getName() + " - " + e.getMessage());
            return ResponseEntity.internalServerError().body(PaymentResponse.builder()
                    .status("ERROR")
                    .message("Internal server error: " + e.getMessage())
                    .build());
        } finally {
            rateLimiter.release();
        }
    }
}
