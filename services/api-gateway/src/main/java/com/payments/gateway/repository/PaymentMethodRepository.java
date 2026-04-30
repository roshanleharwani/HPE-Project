package com.payments.gateway.repository;

import com.payments.gateway.entity.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, UUID> {
    List<PaymentMethodEntity> findByUserId(UUID userId);
}
