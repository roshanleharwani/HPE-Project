package com.payments.persistence.config;

import org.springframework.stereotype.Component;

/**
 * Determines which PostgreSQL shard to route a transaction to
 * based on a consistent hash of the transaction ID.
 *
 * Shard assignment: hash(transactionId) % SHARD_COUNT
 * This ensures the same transactionId always routes to the same shard.
 */
@Component
public class ShardRouter {

    private static final int SHARD_COUNT = 3;

    /**
     * Returns the shard index (0, 1, or 2) for a given transaction ID.
     */
    public int determineShard(String transactionId) {
        if (transactionId == null || transactionId.isEmpty()) {
            return 0;
        }
        // Use Math.abs to handle negative hash codes
        return Math.abs(transactionId.hashCode() % SHARD_COUNT);
    }

    public int getShardCount() {
        return SHARD_COUNT;
    }
}
