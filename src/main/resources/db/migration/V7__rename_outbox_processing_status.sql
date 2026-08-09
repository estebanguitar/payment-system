UPDATE payment_outbox SET status = 'PENDING' WHERE status = 'INIT';
UPDATE payment_outbox SET status = 'COMPLETED' WHERE status = 'PUBLISHED';
COMMENT ON COLUMN payment_outbox.status IS '이벤트 처리 상태(PENDING, RETRY, COMPLETED, FAILED)';
