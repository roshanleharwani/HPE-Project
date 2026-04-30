package com.payments.gateway.controller;

import com.payments.gateway.dto.RefundRequestDTO;
import com.payments.gateway.dto.RefundResponseDTO;
import com.payments.gateway.dto.RefundStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final EntityManager entityManager;

    // All 3 shard JDBC URLs (matching persistence-worker config)
    private static final String[][] SHARDS = {
        {"jdbc:postgresql://postgres-postgresql:5432/postgres", "postgres", "adminpassword"},
        {"jdbc:postgresql://postgres-shard-1:5432/postgres", "postgres", "adminpassword"},
        {"jdbc:postgresql://postgres-shard-2:5432/postgres", "postgres", "adminpassword"}
    };

    @PostMapping
    @Transactional
    public ResponseEntity<?> requestRefund(@RequestBody RefundRequestDTO request) {
        try {
            // 1. Find the transaction across all shards
            Object[] txn = null;
            for (String[] shard : SHARDS) {
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(shard[0], shard[1], shard[2]);
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, amount, currency, CAST(status AS text), created_at FROM transactions WHERE transaction_id = ? LIMIT 1")) {
                    ps.setString(1, request.getTransactionId());
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        txn = new Object[]{
                            (java.util.UUID) rs.getObject("id"),
                            rs.getBigDecimal("amount"),
                            rs.getString("currency"),
                            rs.getString(4),
                            rs.getTimestamp("created_at")
                        };
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to query shard " + shard[0] + ": " + e.getMessage());
                }
            }

            if (txn == null) {
                return ResponseEntity.badRequest().body(RefundResponseDTO.builder()
                        .success(false)
                        .message("Transaction not found: " + request.getTransactionId())
                        .build());
            }

            UUID txnUUID = (UUID) txn[0];
            BigDecimal amount = txn[1] != null ? new BigDecimal(txn[1].toString()) : BigDecimal.ZERO;
            String currency = txn[2] != null ? txn[2].toString() : "INR";
            String status = txn[3] != null ? txn[3].toString() : "";
            LocalDateTime createdAt = null;
            if (txn[4] instanceof java.sql.Timestamp) {
                createdAt = ((java.sql.Timestamp) txn[4]).toLocalDateTime();
            } else if (txn[4] instanceof LocalDateTime) {
                createdAt = (LocalDateTime) txn[4];
            }

            // 2. Check if the transaction is within the last 7 days
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            if (createdAt != null && createdAt.isBefore(sevenDaysAgo)) {
                return ResponseEntity.badRequest().body(RefundResponseDTO.builder()
                        .success(false)
                        .message("Refund window expired. Refunds can only be requested within 7 days of the transaction.")
                        .build());
            }

            // 3. Check if transaction status allows refund (only SETTLED transactions)
            if (!"SETTLED".equalsIgnoreCase(status)) {
                return ResponseEntity.badRequest().body(RefundResponseDTO.builder()
                        .success(false)
                        .message("Only settled transactions can be refunded. Current status: " + status)
                        .build());
            }

            // 4. Create refund record using native query
            UUID refundId = UUID.randomUUID();
            Query insertQuery = entityManager.createNativeQuery(
                    "INSERT INTO refunds (id, transaction_id, amount, currency, reason, status, created_at, updated_at) VALUES (:id, :txnId, :amount, :currency, :reason, 'REQUESTED', NOW(), NOW())");
            insertQuery.setParameter("id", refundId);
            insertQuery.setParameter("txnId", txnUUID);
            insertQuery.setParameter("amount", amount);
            insertQuery.setParameter("currency", currency);
            insertQuery.setParameter("reason", request.getReason());
            insertQuery.executeUpdate();

            return ResponseEntity.ok(RefundResponseDTO.builder()
                    .success(true)
                    .refundId(refundId.toString())
                    .message("Refund request submitted successfully.")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", e.getClass().getSimpleName(),
                           "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRefundStatus(@PathVariable String id) {
        try {
            UUID refundId = UUID.fromString(id);

            Query query = entityManager.createNativeQuery(
                    "SELECT id, transaction_id, amount, reason, CAST(status AS text), created_at, updated_at FROM refunds WHERE id = :refundId");
            query.setParameter("refundId", refundId);

            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();

            if (rows.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Object[] row = rows.get(0);
            LocalDateTime requestDate = null;
            LocalDateTime completedDate = null;
            if (row[5] instanceof Timestamp) requestDate = ((Timestamp) row[5]).toLocalDateTime();
            if (row[6] instanceof Timestamp) completedDate = ((Timestamp) row[6]).toLocalDateTime();

            return ResponseEntity.ok(RefundStatusDTO.builder()
                    .id(row[0] != null ? row[0].toString() : null)
                    .transactionId(row[1] != null ? row[1].toString() : null)
                    .amount(row[2] != null ? new BigDecimal(row[2].toString()) : null)
                    .reason(row[3] != null ? row[3].toString() : null)
                    .status(row[4] != null ? row[4].toString().toLowerCase() : "requested")
                    .requestDate(requestDate)
                    .completedDate(completedDate)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", e.getClass().getSimpleName(),
                           "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
