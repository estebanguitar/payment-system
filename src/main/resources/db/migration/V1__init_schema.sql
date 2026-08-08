-- 1. 고객(사용자) 테이블
CREATE TABLE customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '고객 내부 식별자',
    customer_id VARCHAR(64) NOT NULL UNIQUE COMMENT '외부에 노출되는 고객 고유 식별자',
    name VARCHAR(64) NOT NULL COMMENT '고객 이름',
    email VARCHAR(128) COMMENT '고객 이메일 주소',
    created_at TIMESTAMP NOT NULL COMMENT '고객 생성 일시',
    updated_at TIMESTAMP NOT NULL COMMENT '고객 최종 수정 일시'
);
CREATE UNIQUE INDEX idx_customer_customer_id ON customer(customer_id);

-- 2. 지갑 테이블
CREATE TABLE wallet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '지갑 내부 식별자',
    customer_id VARCHAR(64) NOT NULL UNIQUE COMMENT '지갑을 소유한 고객 고유 식별자',
    balance BIGINT NOT NULL DEFAULT 0 COMMENT '현재 지갑 잔액(원)',
    created_at TIMESTAMP NOT NULL COMMENT '지갑 생성 일시',
    updated_at TIMESTAMP NOT NULL COMMENT '지갑 최종 수정 일시',
    CONSTRAINT fk_wallet_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);
CREATE UNIQUE INDEX idx_wallet_customer_id ON wallet(customer_id);

-- 3. 지갑 거래 이력 테이블
CREATE TABLE wallet_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '지갑 거래 내부 식별자',
    wallet_id BIGINT NOT NULL COMMENT '거래가 발생한 지갑 식별자',
    transaction_type VARCHAR(32) NOT NULL COMMENT '지갑 거래 유형(TOP_UP, PAYMENT, REFUND)',
    amount BIGINT NOT NULL COMMENT '거래 금액(원)',
    balance_after BIGINT NOT NULL COMMENT '거래 반영 후 지갑 잔액(원)',
    payment_id BIGINT COMMENT '관련 결제 식별자',
    cancel_id BIGINT COMMENT '관련 결제 취소 식별자',
    idempotency_key VARCHAR(128) UNIQUE COMMENT '충전 요청 중복 방지용 멱등키',
    created_at TIMESTAMP NOT NULL COMMENT '지갑 거래 생성 일시',
    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallet(id)
);

-- 4. 결제 원장 테이블
CREATE TABLE payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 내부 식별자',
    idempotency_key VARCHAR(128) NOT NULL UNIQUE COMMENT '결제 요청 중복 방지용 멱등키',
    customer_id VARCHAR(64) NOT NULL COMMENT '결제를 요청한 고객 고유 식별자',
    amount BIGINT NOT NULL COMMENT '원 결제 금액(원)',
    status VARCHAR(32) NOT NULL COMMENT '결제 상태(PENDING, COMPLETED, PARTIALLY_CANCELED, CANCELED, FAILED)',
    failure_reason VARCHAR(64) COMMENT '결제 실패 사유',
    accumulated_cancel_amount BIGINT NOT NULL DEFAULT 0 COMMENT '완료된 취소의 누적 금액(원)',
    created_at TIMESTAMP NOT NULL COMMENT '결제 생성 일시',
    updated_at TIMESTAMP NOT NULL COMMENT '결제 최종 수정 일시',
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);
CREATE INDEX idx_payment_customer_created ON payment(customer_id, created_at DESC);
CREATE INDEX idx_payment_ops_search ON payment(status, created_at DESC);

-- 5. 취소 거래 테이블
CREATE TABLE payment_cancel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 취소 내부 식별자',
    payment_id BIGINT NOT NULL COMMENT '취소 대상 원 결제 식별자',
    idempotency_key VARCHAR(128) NOT NULL UNIQUE COMMENT '취소 요청 중복 방지용 멱등키',
    cancel_type VARCHAR(32) NOT NULL COMMENT '취소 유형(FULL, PARTIAL)',
    amount BIGINT NOT NULL COMMENT '취소 요청 금액(원)',
    status VARCHAR(32) NOT NULL COMMENT '취소 처리 상태(PENDING, COMPLETED, FAILED)',
    failure_reason VARCHAR(64) COMMENT '취소 실패 사유',
    pg_cancel_transaction_id VARCHAR(128) COMMENT '외부 PG 취소 거래 식별자',
    created_at TIMESTAMP NOT NULL COMMENT '취소 요청 생성 일시',
    completed_at TIMESTAMP COMMENT '취소 처리 완료 일시',
    CONSTRAINT fk_cancel_payment FOREIGN KEY (payment_id) REFERENCES payment(id)
);

-- 6. 외부 PG 응답 로그 테이블
CREATE TABLE pg_response_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '외부 PG 응답 로그 내부 식별자',
    payment_id BIGINT NOT NULL COMMENT '응답과 관련된 결제 식별자',
    pg_transaction_id VARCHAR(128) COMMENT '외부 PG 거래 식별자',
    pg_response_code VARCHAR(32) COMMENT '외부 PG 응답 코드',
    encrypted_payload CLOB NOT NULL COMMENT '암호화된 외부 PG 응답 원문',
    received_at TIMESTAMP NOT NULL COMMENT '외부 PG 응답 수신 일시',
    CONSTRAINT fk_pg_log_payment FOREIGN KEY (payment_id) REFERENCES payment(id)
);

-- 7. 결제 아웃박스 테이블 (Outbox-Ready Pattern)
CREATE TABLE payment_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '아웃박스 이벤트 내부 식별자',
    payment_id BIGINT NOT NULL COMMENT '이벤트 대상 결제 식별자',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '이벤트 대상 결제의 멱등키',
    event_type VARCHAR(64) NOT NULL COMMENT '아웃박스 이벤트 유형',
    payload CLOB NOT NULL COMMENT '이벤트 직렬화 데이터',
    status VARCHAR(32) NOT NULL COMMENT '이벤트 발행 상태(INIT, PUBLISHED, FAILED)',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '이벤트 발행 재시도 횟수',
    created_at TIMESTAMP NOT NULL COMMENT '아웃박스 이벤트 생성 일시',
    updated_at TIMESTAMP NOT NULL COMMENT '아웃박스 이벤트 최종 수정 일시'
);
CREATE INDEX idx_outbox_status_created ON payment_outbox(status, created_at);
