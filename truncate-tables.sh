#!/bin/bash

# truncate-tables.sh
# This script truncates all transaction-related tables across the primary DB and all shards.
# Useful for resetting the database state between load tests.

echo "Truncating tables on Primary DB (postgres-postgresql-0)..."
kubectl exec postgres-postgresql-0 -- psql -U postgres -d postgres -c "TRUNCATE TABLE payment_intents, transactions, ledger_entries, payment_events, refunds, audit_logs CASCADE;"

echo "Truncating tables on Shard 1 (postgres-shard-1-0)..."
kubectl exec postgres-shard-1-0 -- psql -U postgres -d postgres -c "TRUNCATE TABLE payment_intents, transactions, ledger_entries, payment_events, refunds, audit_logs CASCADE;"

echo "Truncating tables on Shard 2 (postgres-shard-2-0)..."
kubectl exec postgres-shard-2-0 -- psql -U postgres -d postgres -c "TRUNCATE TABLE payment_intents, transactions, ledger_entries, payment_events, refunds, audit_logs CASCADE;"

echo "Truncation complete!"
