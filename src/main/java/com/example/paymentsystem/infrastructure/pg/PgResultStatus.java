package com.example.paymentsystem.infrastructure.pg;

/** 외부 PG가 정상 응답으로 반환할 승인 또는 거절 결과를 정의한다. */
public enum PgResultStatus {
    APPROVED,
    REJECTED
}
