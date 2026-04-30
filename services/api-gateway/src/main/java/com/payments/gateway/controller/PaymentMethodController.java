package com.payments.gateway.controller;

import com.payments.gateway.dto.AddPaymentMethodRequest;
import com.payments.gateway.dto.AddPaymentMethodResponse;
import com.payments.gateway.dto.PaymentMethodDTO;
import com.payments.gateway.entity.PaymentMethodEntity;
import com.payments.gateway.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodRepository paymentMethodRepository;

    // Default user ID from seed data
    private static final UUID DEFAULT_USER_ID = UUID.fromString("a1b2c3d4-e5f6-7890-1234-56789abcdef0");

    @PostMapping
    public ResponseEntity<AddPaymentMethodResponse> addPaymentMethod(@RequestBody AddPaymentMethodRequest request) {
        PaymentMethodEntity entity = new PaymentMethodEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(DEFAULT_USER_ID);
        entity.setMethodType(request.getType() != null ? request.getType().toUpperCase() : "CARD");
        entity.setProvider(request.getName());
        entity.setLastFour(request.getLastFour());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        paymentMethodRepository.save(entity);

        return ResponseEntity.ok(AddPaymentMethodResponse.builder()
                .success(true)
                .paymentMethodId(entity.getId().toString())
                .build());
    }

    @GetMapping
    public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethods() {
        List<PaymentMethodDTO> methods = paymentMethodRepository.findByUserId(DEFAULT_USER_ID)
                .stream()
                .map(entity -> PaymentMethodDTO.builder()
                        .id(entity.getId().toString())
                        .methodType(entity.getMethodType())
                        .provider(entity.getProvider())
                        .lastFour(entity.getLastFour())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(methods);
    }
}
