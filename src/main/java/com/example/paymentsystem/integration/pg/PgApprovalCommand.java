package com.example.paymentsystem.integration.pg;

import lombok.Builder;
import lombok.Getter;

/** 결제 승인을 위해 외부 PG에 전달할 Application 출력 명령이다. */
@Getter
@Builder
public class PgApprovalCommand {
    private final Long paymentId;
    private final String idempotencyKey;
    private final String customerId;
    private final long amount;
}
