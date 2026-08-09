package com.example.paymentsystem.infrastructure.pg;

import lombok.Builder;
import lombok.Getter;

/** 가상 PG 결제 승인 결과와 암호화 전 원문을 전달한다. */
@Getter
@Builder
public class PgApprovalResponse {
    private final PgResultStatus status;
    private final String pgTransactionId;
    private final String responseCode;
    private final String rawPayload;
}
