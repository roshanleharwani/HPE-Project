package com.payments.persistence.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class TransactionRepository {

    private static final Logger log = LoggerFactory.getLogger(TransactionRepository.class);

    /**
     * Update payment_intent status (e.g., CREATED -> SETTLED).
     */
    public void updatePaymentIntentStatus(JdbcTemplate jdbc, String paymentIntentId, String status) {
        try {
            jdbc.update(
                    "UPDATE payment_intents SET status = ?::payment_intent_status, updated_at = NOW() WHERE id = ?::uuid",
                    status, paymentIntentId);
        } catch (Exception e) {
            log.warn("Could not update payment_intent {}: {}", paymentIntentId, e.getMessage());
        }
    }

    public String upsertTransaction(JdbcTemplate jdbc, String transactionId, String paymentIntentId,
            String paymentMethodId, BigDecimal amount,
            String currency, String status) {
        try {
            // Check if transaction already exists
            List<Map<String, Object>> existing = jdbc.queryForList(
                    "SELECT id FROM transactions WHERE transaction_id = ?",
                    transactionId);

            if (!existing.isEmpty()) {
                String existingId = existing.get(0).get("id").toString();
                jdbc.update(
                        "UPDATE transactions SET status = ?::transaction_status, updated_at = NOW() WHERE id = ?::uuid",
                        status, existingId);
                return existingId;
            }

            // Insert new transaction
            String id = UUID.randomUUID().toString();
            String piId = (paymentIntentId == null || paymentIntentId.isEmpty()) ? null : paymentIntentId;
            String pmId = (paymentMethodId == null || paymentMethodId.isEmpty()) ? null : paymentMethodId;

            jdbc.update(
                    "INSERT INTO transactions (id, payment_intent_id, payment_method_id, transaction_id, amount, currency, status, created_at, updated_at) "
                            +
                            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?, ?::transaction_status, NOW(), NOW())",
                    id, piId, pmId, transactionId, amount, currency, status);
            return id;
        } catch (Exception e) {
            log.error("Failed to upsert transaction {}: {}", transactionId, e.getMessage());
            throw e;
        }
    }

    public String findOrCreateAccount(JdbcTemplate jdbc, String userId, String accountType, String currency) {
        try {
            List<Map<String, Object>> existing;

            if (userId != null && !userId.isEmpty()) {
                existing = jdbc.queryForList(
                        "SELECT id FROM accounts WHERE user_id = ?::uuid AND account_type = ?",
                        userId, accountType);
            } else {
                existing = jdbc.queryForList(
                        "SELECT id FROM accounts WHERE user_id IS NULL AND account_type = ?",
                        accountType);
            }

            if (!existing.isEmpty()) {
                return existing.get(0).get("id").toString();
            }

            // Create new account
            String id = UUID.randomUUID().toString();
            String uid = (userId == null || userId.isEmpty()) ? null : userId;
            jdbc.update(
                    "INSERT INTO accounts (id, user_id, account_type, currency, created_at) VALUES (?::uuid, ?::uuid, ?, ?, NOW())",
                    id, uid, accountType, currency);
            return id;
        } catch (Exception e) {
            log.error("Failed to find/create account for userId={}, type={}: {}", userId, accountType, e.getMessage());
            throw e;
        }
    }

    public void insertLedgerEntry(JdbcTemplate jdbc, String transactionId, String accountId,
            String entryType, BigDecimal amount, String currency) {
        try {
            String id = UUID.randomUUID().toString();
            jdbc.update(
                    "INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, currency, created_at) "
                            +
                            "VALUES (?::uuid, ?::uuid, ?::uuid, ?::ledger_entry_type, ?, ?, NOW())",
                    id, transactionId, accountId, entryType, amount, currency);
        } catch (Exception e) {
            log.error("Failed to insert ledger entry: {}", e.getMessage());
            throw e;
        }
    }

    public void insertPaymentEvent(JdbcTemplate jdbc, String transactionId, String eventType,
            String serviceName, String eventData) {
        try {
            String id = UUID.randomUUID().toString();
            jdbc.update(
                    "INSERT INTO payment_events (id, transaction_id, event_type, service_name, event_data, created_at) "
                            +
                            "VALUES (?::uuid, ?::uuid, ?, ?, ?::jsonb, NOW())",
                    id, transactionId, eventType, serviceName, eventData);
        } catch (Exception e) {
            log.error("Failed to insert payment event: {}", e.getMessage());
            throw e;
        }
    }

    public void insertAuditLog(JdbcTemplate jdbc, String serviceName, String action, String metadata) {
        try {
            String id = UUID.randomUUID().toString();
            jdbc.update(
                    "INSERT INTO audit_logs (id, service_name, action, metadata, created_at) " +
                            "VALUES (?::uuid, ?, ?, ?::jsonb, NOW())",
                    id, serviceName, action, metadata);
        } catch (Exception e) {
            log.error("Failed to insert audit log: {}", e.getMessage());
        }
    }

    public void updateAccountBalance(JdbcTemplate jdbc, String accountId, BigDecimal amount) {
        try {
            jdbc.update("UPDATE accounts SET balance = balance + ? WHERE id = ?::uuid", amount, accountId);
        } catch (Exception e) {
            log.error("Failed to update account balance for {}: {}", accountId, e.getMessage());
            throw e;
        }
    }
}
