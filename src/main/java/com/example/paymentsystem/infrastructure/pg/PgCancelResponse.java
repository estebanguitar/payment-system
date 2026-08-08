package com.example.paymentsystem.infrastructure.pg;

import lombok.Builder;
import lombok.Getter;

/** 가상 PG 취소 결과와 암호화 전 원문을 전달한다. */
@Getter
@Builder
public class PgCancelResponse {
    private final PgResultStatus status;
    private final String pgCancelTransactionId;
    private final String responseCode;
    private final String rawPayload;
}
