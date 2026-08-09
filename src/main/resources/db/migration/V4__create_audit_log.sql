-- 클라이언트 API 요청 감사 로그 테이블
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '감사 로그 내부 식별자',
    trace_id VARCHAR(64) NOT NULL COMMENT '요청 단위 추적 식별자',
    client_id VARCHAR(100) NOT NULL COMMENT '정규화된 클라이언트 식별자',
    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP 요청 메서드',
    request_path VARCHAR(255) NOT NULL COMMENT 'Path Variable 값이 제거된 정규화 요청 경로',
    response_status INT NOT NULL COMMENT '최종 HTTP 응답 상태 코드',
    error_code VARCHAR(64) COMMENT '외부에 공개 가능한 표준 오류 코드',
    success BOOLEAN NOT NULL COMMENT 'HTTP 상태 코드 400 미만 여부',
    duration_ms BIGINT NOT NULL COMMENT '요청 처리 시간(밀리초)',
    requested_at TIMESTAMP(6) NOT NULL COMMENT '요청 처리 시작 일시',
    completed_at TIMESTAMP(6) NOT NULL COMMENT '요청 처리 완료 일시'
);

CREATE INDEX idx_audit_requested ON audit_log(requested_at DESC, id DESC);
CREATE INDEX idx_audit_client_requested ON audit_log(client_id, requested_at DESC);
CREATE INDEX idx_audit_path_requested ON audit_log(request_path, requested_at DESC);
CREATE INDEX idx_audit_trace ON audit_log(trace_id);
CREATE INDEX idx_audit_status_requested ON audit_log(response_status, requested_at DESC);
