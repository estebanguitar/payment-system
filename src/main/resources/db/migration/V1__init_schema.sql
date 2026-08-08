-- 1. 고객(사용자) 테이블
CREATE TABLE customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX idx_customer_customer_id ON customer(customer_id);

-- 2. 지갑 테이블
CREATE TABLE wallet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL UNIQUE,
    balance BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wallet_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);
CREATE UNIQUE INDEX idx_wallet_customer_id ON wallet(customer_id);

-- 3. 지갑 거래 이력 테이블
CREATE TABLE wallet_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    payment_id BIGINT,
    cancel_id BIGINT,
    idempotency_key VARCHAR(128) UNIQUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallet(id)
);

-- 4. 결제 원장 테이블
CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    customer_id VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(64),
    accumulated_cancel_amount BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);
CREATE INDEX idx_payment_customer_created ON payment(customer_id, created_at DESC);
CREATE INDEX idx_payment_ops_search ON payment(status, created_at DESC);

-- 5. 취소 거래 테이블
CREATE TABLE payment_cancel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    cancel_type VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(64),
    pg_cancel_transaction_id VARCHAR(128),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_cancel_payment FOREIGN KEY (payment_id) REFERENCES payment(id)
);

-- 6. 외부 PG 응답 로그 테이블
CREATE TABLE pg_response_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    pg_transaction_id VARCHAR(128),
    pg_response_code VARCHAR(32),
    encrypted_payload CLOB NOT NULL,
    received_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_pg_log_payment FOREIGN KEY (payment_id) REFERENCES payment(id)
);

-- 7. 결제 아웃박스 테이블 (Outbox-Ready Pattern)
CREATE TABLE payment_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload CLOB NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_outbox_status_created ON payment_outbox(status, created_at);
