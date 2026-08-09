package com.example.paymentsystem.infrastructure.pg;

import lombok.Getter;

/** Application 레이어가 시스템 실패로 변환할 수 있도록 PG 기술 오류 유형을 보존한다. */
@Getter
public class PgClientException extends RuntimeException {

    private final PgErrorType errorType;

    /** 원문이나 인증정보를 포함하지 않은 표준 PG 오류를 생성한다. */
    public PgClientException(PgErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    /** 직렬화 등 내부 원인을 보존한 표준 PG 오류를 생성한다. */
    public PgClientException(PgErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
}
