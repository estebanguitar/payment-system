package com.example.paymentsystem.integration.pg;

import lombok.Getter;

/** PG 어댑터가 반환하는 표준 기술 예외다. */
@Getter
public class PgClientException extends RuntimeException {
    private final PgErrorType errorType;

    /** 분류된 PG 오류를 생성한다. */
    public PgClientException(PgErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    /** 내부 원인을 보존한 PG 오류를 생성한다. */
    public PgClientException(PgErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
}
