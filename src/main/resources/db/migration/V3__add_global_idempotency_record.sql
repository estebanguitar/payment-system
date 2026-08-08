CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(128) PRIMARY KEY COMMENT '시스템 전체에서 유일한 클라이언트 멱등키',
    request_type VARCHAR(32) NOT NULL COMMENT '멱등 요청 유형(PAYMENT, PAYMENT_CANCEL, WALLET_TOP_UP)',
    request_fingerprint VARCHAR(64) NOT NULL COMMENT '정규화된 요청 데이터의 SHA-256 지문',
    resource_id BIGINT COMMENT '요청 처리로 생성된 업무 리소스 식별자',
    created_at TIMESTAMP NOT NULL COMMENT '멱등키 선점 일시',
    updated_at TIMESTAMP NOT NULL COMMENT '멱등 처리 결과 최종 수정 일시'
);

CREATE INDEX idx_idempotency_record_type_created
    ON idempotency_record(request_type, created_at);
