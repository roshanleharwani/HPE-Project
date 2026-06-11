package com.payments.gateway.controller;

import com.payments.gateway.dto.TransactionDTO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private static final String[][] SHARDS = {
        {"jdbc:postgresql://postgres-postgresql:5432/postgres", "postgres", "adminpassword"},
        {"jdbc:postgresql://postgres-shard-1:5432/postgres", "postgres", "adminpassword"},
        {"jdbc:postgresql://postgres-shard-2:5432/postgres", "postgres", "adminpassword"}
    };

    private static final String SELECT_ALL = 
        "SELECT id, payment_intent_id, transaction_id, amount, currency, CAST(status AS text), created_at FROM transactions ORDER BY created_at DESC LIMIT 50";

    private static final String SELECT_BY_ID = 
        "SELECT id, payment_intent_id, transaction_id, amount, currency, CAST(status AS text), created_at FROM transactions WHERE transaction_id = ? LIMIT 1";

    private final List<HikariDataSource> dataSources = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (String[] shard : SHARDS) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(shard[0]);
            config.setUsername(shard[1]);
            config.setPassword(shard[2]);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(5000);
            config.setInitializationFailTimeout(-1); // Don't fail startup if shard is temporarily down
            dataSources.add(new HikariDataSource(config));
        }
    }

    @PreDestroy
    public void cleanup() {
        for (HikariDataSource ds : dataSources) {
            if (ds != null) {
                ds.close();
            }
        }
    }

    @GetMapping
    public ResponseEntity<?> getTransactions() {
        try {
            List<TransactionDTO> allTransactions = new ArrayList<>();

            for (HikariDataSource ds : dataSources) {
                try {
                    allTransactions.addAll(queryTransactions(ds, SELECT_ALL));
                } catch (Exception e) {
                    log.warn("Failed to query shard {}: {}", ds.getJdbcUrl(), e.getMessage());
                }
            }

            allTransactions.sort((a, b) -> {
                if (b.getCreatedAt() == null) return -1;
                if (a.getCreatedAt() == null) return 1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

            return ResponseEntity.ok(allTransactions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", e.getClass().getSimpleName(),
                           "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        try {
            for (HikariDataSource ds : dataSources) {
                try (Connection conn = ds.getConnection();
                     PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
                    ps.setString(1, id);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return ResponseEntity.ok(mapRow(rs));
                    }
                } catch (Exception e) {
                    log.warn("Failed to query shard {}: {}", ds.getJdbcUrl(), e.getMessage());
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", e.getClass().getSimpleName(),
                           "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    private List<TransactionDTO> queryTransactions(HikariDataSource ds, String sql) throws SQLException {
        List<TransactionDTO> results = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    private TransactionDTO mapRow(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null;
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            createdAt = ts.toLocalDateTime();
        }

        String txnId = rs.getString("transaction_id");
        String id = rs.getString("id");

        return TransactionDTO.builder()
                .transactionId(txnId != null ? txnId : id)
                .paymentIntentId(rs.getString("payment_intent_id"))
                .amount(rs.getBigDecimal("amount"))
                .currency(rs.getString("currency"))
                .status(rs.getString(6))
                .createdAt(createdAt)
                .build();
    }
}
