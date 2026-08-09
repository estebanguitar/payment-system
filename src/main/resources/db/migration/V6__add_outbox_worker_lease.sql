ALTER TABLE payment_outbox ADD COLUMN worker_id VARCHAR(128);
ALTER TABLE payment_outbox ADD COLUMN lease_until TIMESTAMP;
ALTER TABLE payment_outbox ADD COLUMN next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX idx_outbox_claim ON payment_outbox(status, next_attempt_at, lease_until, created_at);
