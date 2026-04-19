package com.payments.persistence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.persistence.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Orchestrates the persistence of a settled payment into PostgreSQL.
 * Per arch.md, this writes to:
 *   - transactions
 *   - ledger_entries
 *   - payment_events
 *   - audit_logs
 */
@Service
public class PersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);
    private final TransactionRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PersistenceService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void persistPayment(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // The settlement service wraps the original payload under "original_payload"
            JsonNode payload = root.has("original_payload") ? root.get("original_payload") : root;

            String transactionId = payload.path("transactionId").asText("");
            String paymentIntentId = payload.path("paymentIntentId").asText("");
            String paymentMethodId = payload.path("paymentMethodId").asText("");
            String userId = payload.path("userId").asText("");
            String currency = payload.path("currency").asText("INR");

            BigDecimal amount = BigDecimal.ZERO;
            if (payload.has("amount")) {
                amount = new BigDecimal(payload.path("amount").asText("0"));
            }

            BigDecimal feeAmount = BigDecimal.ZERO;
            if (payload.has("feeAmount")) {
                feeAmount = new BigDecimal(payload.path("feeAmount").asText("0"));
            }

            BigDecimal settlementAmount = BigDecimal.ZERO;
            if (payload.has("settlementAmount")) {
                settlementAmount = new BigDecimal(payload.path("settlementAmount").asText("0"));
            }

            log.info("Persisting settled payment: txnId={}, amount={}, currency={}", transactionId, amount, currency);

            // 1. Update payment_intent status to SETTLED
            if (!paymentIntentId.isEmpty()) {
                repository.updatePaymentIntentStatus(paymentIntentId, "SETTLED");
                log.info("Updated payment_intent {} status to SETTLED", paymentIntentId);
            }

            // 2. Insert or update transaction record
            String txnDbId = repository.upsertTransaction(
                    transactionId, paymentIntentId, paymentMethodId,
                    amount, currency, "SETTLED"
            );
            log.info("Upserted transaction record, DB id: {}", txnDbId);

            // 3. Create ledger entries (double-entry accounting)
            // Per arch.md: DEBIT from user wallet, CREDIT to merchant wallet
            if (txnDbId != null) {
                // Find user and merchant accounts
                String userAccountId = repository.findOrCreateAccount(userId, "USER_WALLET", currency);
                // Use a platform merchant account
                String merchantAccountId = repository.findOrCreateAccount(null, "MERCHANT_WALLET", currency);
                String feeAccountId = repository.findOrCreateAccount(null, "PLATFORM_FEES", currency);

                // Debit from user
                repository.insertLedgerEntry(txnDbId, userAccountId, "DEBIT", amount, currency);
                // Credit to merchant (settlement amount = amount - fees)
                repository.insertLedgerEntry(txnDbId, merchantAccountId, "CREDIT", settlementAmount, currency);
                // Credit fees to platform
                if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
                    repository.insertLedgerEntry(txnDbId, feeAccountId, "CREDIT", feeAmount, currency);
                }
                log.info("Created ledger entries for transaction {}", transactionId);
            }

            // 4. Insert payment event
            repository.insertPaymentEvent(txnDbId, "PAYMENT_SETTLED", "persistence-worker",
                    message);
            log.info("Created payment event for transaction {}", transactionId);

            // 5. Insert audit log
            repository.insertAuditLog("persistence-worker", "payment_persisted", message);
            log.info("Created audit log for transaction {}", transactionId);

        } catch (Exception e) {
            log.error("Error persisting payment: {}", e.getMessage(), e);
            throw new RuntimeException("Persistence failed for payment", e);
        }
    }
}
