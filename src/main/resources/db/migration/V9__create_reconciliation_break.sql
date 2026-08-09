CREATE TABLE reconciliation_break (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '대사 불일치 식별자',
    break_key VARCHAR(200) NOT NULL UNIQUE COMMENT '동일 불일치 중복 방지 키',
    break_type VARCHAR(64) NOT NULL COMMENT '불일치 유형',
    payment_id BIGINT COMMENT '관련 결제 식별자',
    wallet_id BIGINT COMMENT '관련 지갑 식별자',
    expected_value VARCHAR(200) NOT NULL COMMENT '기대 상태 또는 금액',
    actual_value VARCHAR(200) NOT NULL COMMENT '실제 상태 또는 금액',
    description VARCHAR(500) NOT NULL COMMENT '불일치 설명',
    detected_at TIMESTAMP(6) NOT NULL COMMENT '최초 탐지 시각'
);

CREATE INDEX idx_reconciliation_break_detected
    ON reconciliation_break(detected_at DESC, id DESC);
