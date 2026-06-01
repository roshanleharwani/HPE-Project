-- Ensure the test user exists
INSERT INTO users (id, email, full_name, created_at, updated_at)
VALUES ('a1b2c3d4-e5f6-7890-1234-56789abcdef0', 'roshan@gmail.com', 'Roshan', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Ensure the test payment method exists
INSERT INTO payment_methods (id, user_id, method_type, provider, token, last_four, expiry_month, expiry_year, created_at, updated_at)
VALUES ('c3d4e5f6-a7b8-9012-3456-789abcdef012', 'a1b2c3d4-e5f6-7890-1234-56789abcdef0', 'CARD', 'VISA', 'tok_visa_load_test', '4242', 12, 2027, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Ensure transaction partition exists for current month (MAY 2026)
CREATE TABLE IF NOT EXISTS transactions_2026_05
PARTITION OF transactions
FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

-- Create 2,000,000 payment intents to survive a 3-minute test at 10k RPS
INSERT INTO payment_intents (id, user_id, amount, currency, status, description, created_at, updated_at)
SELECT
    (lpad(i::text, 8, '0') || '-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    'a1b2c3d4-e5f6-7890-1234-56789abcdef0'::uuid,
    500.00,
    'INR',
    'CREATED',
    'Load test intent #' || i,
    NOW(),
    NOW()
FROM generate_series(1, 2000000) AS i
ON CONFLICT (id) DO NOTHING;

SELECT 'Seeded payment_intents: ' || count(*) FROM payment_intents;
