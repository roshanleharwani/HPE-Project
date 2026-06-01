package com.payments.persistence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.persistence.config.ShardRouter;
import com.payments.persistence.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Orchestrates the persistence of a settled payment into the correct
 * PostgreSQL shard based on transactionId hash.
 *
 * Shard routing: shard_number = hash(transactionId) % 3
 */
@Service
public class PersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);
    private final TransactionRepository repository;
    private final ShardRouter shardRouter;
    private final Map<Integer, JdbcTemplate> shardedJdbcTemplates;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PersistenceService(TransactionRepository repository,
                              ShardRouter shardRouter,
                              Map<Integer, JdbcTemplate> shardedJdbcTemplates) {
        this.repository = repository;
        this.shardRouter = shardRouter;
        this.shardedJdbcTemplates = shardedJdbcTemplates;
    }

    public void persistPayment(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // The settlement service wraps the original payload under "original_payload".
            // It should be a JSON object, but defensively handle the case where it arrives
            // as a JSON string (double-serialised) — e.g., during rolling upgrades or from
            // old settlement pods that used string-concatenation instead of nlohmann::json.
            JsonNode payloadNode = root.has("original_payload") ? root.get("original_payload") : root;
            JsonNode payload;
            if (payloadNode.isTextual()) {
                // Double-encoded: the settlement service stringified the inner JSON.
                // Parse it again to get the real object.
                payload = objectMapper.readTree(payloadNode.asText());
            } else {
                payload = payloadNode;
            }

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

            // ── SHARD ROUTING ──
            int shardNum = shardRouter.determineShard(transactionId);
            JdbcTemplate jdbc = shardedJdbcTemplates.get(shardNum);
            log.info("Routing txn {} to shard-{} (hash={})", transactionId, shardNum, transactionId.hashCode());

            // 1. Update payment_intent status to SETTLED
            if (!paymentIntentId.isEmpty()) {
                repository.updatePaymentIntentStatus(jdbc, paymentIntentId, "SETTLED");
                log.info("[shard-{}] Updated payment_intent {} → SETTLED", shardNum, paymentIntentId);
            }

            // 2. Insert or update transaction record
            String txnDbId = repository.upsertTransaction(
                    jdbc, transactionId, paymentIntentId, paymentMethodId,
                    amount, currency, "SETTLED"
            );
            log.info("[shard-{}] Upserted transaction {}, DB id: {}", shardNum, transactionId, txnDbId);

            // 3. Create ledger entries (double-entry accounting)
            if (txnDbId != null) {
                String userAccountId = repository.findOrCreateAccount(jdbc, userId, "USER_WALLET", currency);
                String merchantAccountId = repository.findOrCreateAccount(jdbc, null, "MERCHANT_WALLET", currency);
                String feeAccountId = repository.findOrCreateAccount(jdbc, null, "PLATFORM_FEES", currency);

                // Debit from user
                repository.insertLedgerEntry(jdbc, txnDbId, userAccountId, "DEBIT", amount, currency);
                // Credit to merchant
                repository.insertLedgerEntry(jdbc, txnDbId, merchantAccountId, "CREDIT", settlementAmount, currency);
                // Credit fees to platform
                if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
                    repository.insertLedgerEntry(jdbc, txnDbId, feeAccountId, "CREDIT", feeAmount, currency);
                }
                log.info("[shard-{}] Created ledger entries for {}", shardNum, transactionId);
            }

            // 4. Insert payment event
            repository.insertPaymentEvent(jdbc, txnDbId, "PAYMENT_SETTLED", "persistence-worker", message);

            // 5. Insert audit log
            repository.insertAuditLog(jdbc, "persistence-worker", "payment_persisted", message);

            log.info("[shard-{}] ✓ Payment {} fully persisted", shardNum, transactionId);

        } catch (Exception e) {
            log.error("Error persisting payment: {}", e.getMessage(), e);
            throw new RuntimeException("Persistence failed for payment", e);
        }
    }
}
