package com.example.paymentsystem.integration.pg;

import lombok.Builder;
import lombok.Getter;

/** 외부 PG의 취소 결과와 암호화 전 원문을 전달한다. */
@Getter
@Builder
public class PgCancelResult {
    private final PgResultStatus status;
    private final String pgCancelTransactionId;
    private final String responseCode;
    private final String rawPayload;
}
