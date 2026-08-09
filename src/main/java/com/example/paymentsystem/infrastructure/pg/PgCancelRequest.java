package com.example.paymentsystem.infrastructure.pg;

import lombok.Builder;
import lombok.Getter;

/** 원 결제의 전액 또는 부분 취소를 가상 PG에 요청하는 데이터다. */
@Getter
@Builder
public class PgCancelRequest {
    private final Long paymentId;
    private final Long cancelId;
    private final String idempotencyKey;
    private final long amount;
    private final PgScenario scenario;
}
