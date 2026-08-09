ALTER TABLE audit_log
    ADD COLUMN encrypted_payload VARCHAR(16384) COMMENT '호출자와 요청·응답 원문의 AES-256-GCM 암호문';

ALTER TABLE audit_log
    ADD COLUMN payload_truncated BOOLEAN NOT NULL DEFAULT FALSE COMMENT '크기 제한으로 원문이 잘렸는지 여부';
