package com.example.paymentsystem.integration.pg;

import lombok.Builder;
import lombok.Getter;

/** 결제 취소를 위해 외부 PG에 전달할 Application 출력 명령이다. */
@Getter
@Builder
public class PgCancelCommand {
    private final Long paymentId;
    private final Long cancelId;
    private final String idempotencyKey;
    private final long amount;
}
