package com.example.paymentsystem.infrastructure.pg;

import lombok.Builder;
import lombok.Getter;

/** 결제 승인을 위해 가상 PG에 전달할 내부 요청 데이터다. */
@Getter
@Builder
public class PgApprovalRequest {
    private final Long paymentId;
    private final String idempotencyKey;
    private final String customerId;
    private final long amount;
    private final PgScenario scenario;
}
