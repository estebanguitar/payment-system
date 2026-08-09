package com.example.paymentsystem.infrastructure.pg;

/** 가상 PG가 결정적으로 재현할 승인·거절·장애 시나리오를 정의한다. */
public enum PgScenario {
    APPROVED,
    REJECTED,
    ERROR,
    TIMEOUT
}
