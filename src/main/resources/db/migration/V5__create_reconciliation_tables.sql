CREATE TABLE reconciliation_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '대사 실행 식별자',
    run_key VARCHAR(160) NOT NULL UNIQUE COMMENT '검사 범위 기반 멱등 실행 키',
    status VARCHAR(32) NOT NULL COMMENT '대사 실행 상태',
    range_start TIMESTAMP NOT NULL COMMENT '검사 범위 시작 일시',
    range_end TIMESTAMP NOT NULL COMMENT '검사 범위 종료 일시',
    started_at TIMESTAMP NOT NULL COMMENT '실행 시작 일시',
    completed_at TIMESTAMP COMMENT '실행 완료 일시',
    checked_count BIGINT NOT NULL DEFAULT 0 COMMENT '정상 완료한 검사기 수',
    mismatch_count BIGINT NOT NULL DEFAULT 0 COMMENT '발견한 불일치 수',
    failed_count BIGINT NOT NULL DEFAULT 0 COMMENT '실패한 검사기 수',
    trigger_type VARCHAR(32) NOT NULL COMMENT '실행 요청 유형',
    requested_by VARCHAR(64) COMMENT '수동 실행 요청자 식별자'
);

CREATE TABLE reconciliation_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '대사 불일치 Case 식별자',
    case_key VARCHAR(200) NOT NULL UNIQUE COMMENT '불일치 유형과 업무 식별자 기반 멱등 키',
    latest_run_id BIGINT NOT NULL COMMENT '마지막으로 불일치를 확인한 실행 식별자',
    mismatch_type VARCHAR(64) NOT NULL COMMENT '불일치 유형',
    severity VARCHAR(16) NOT NULL COMMENT '불일치 심각도',
    status VARCHAR(32) NOT NULL COMMENT '운영 판정 상태',
    customer_id VARCHAR(64) COMMENT '관련 고객 식별자',
    wallet_id BIGINT COMMENT '관련 지갑 식별자',
    payment_id BIGINT COMMENT '관련 결제 식별자',
    cancel_id BIGINT COMMENT '관련 취소 식별자',
    outbox_id BIGINT COMMENT '관련 아웃박스 식별자',
    expected_value VARCHAR(256) COMMENT '마스킹된 기대 값',
    actual_value VARCHAR(256) COMMENT '마스킹된 실제 값',
    evidence CLOB NOT NULL COMMENT '민감정보를 제외한 버전 포함 JSON 근거',
    first_detected_at TIMESTAMP NOT NULL COMMENT '최초 탐지 일시',
    last_detected_at TIMESTAMP NOT NULL COMMENT '최근 탐지 일시',
    resolved_at TIMESTAMP COMMENT '재검증으로 해결된 일시',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '운영 상태 변경 낙관적 잠금 버전',
    CONSTRAINT fk_reconciliation_case_run FOREIGN KEY (latest_run_id) REFERENCES reconciliation_run(id)
);
CREATE INDEX idx_reconciliation_case_search ON reconciliation_case(status, severity, mismatch_type, last_detected_at DESC);

CREATE TABLE reconciliation_action_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '운영 조치 이력 식별자',
    case_id BIGINT NOT NULL COMMENT '대상 불일치 Case 식별자',
    action_type VARCHAR(48) NOT NULL COMMENT '운영 조치 유형',
    from_status VARCHAR(32) NOT NULL COMMENT '조치 전 Case 상태',
    to_status VARCHAR(32) NOT NULL COMMENT '조치 후 Case 상태',
    operator_id VARCHAR(64) NOT NULL COMMENT '운영자 식별자',
    reason VARCHAR(500) NOT NULL COMMENT '운영 조치 사유',
    external_reference VARCHAR(200) COMMENT '외부 승인 또는 작업 참조',
    created_at TIMESTAMP NOT NULL COMMENT '조치 기록 일시',
    CONSTRAINT fk_reconciliation_action_case FOREIGN KEY (case_id) REFERENCES reconciliation_case(id)
);
CREATE INDEX idx_reconciliation_action_case ON reconciliation_action_history(case_id, created_at);
