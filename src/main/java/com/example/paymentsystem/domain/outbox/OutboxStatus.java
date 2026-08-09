package com.example.paymentsystem.domain.outbox;

/** 아웃박스 이벤트의 대기·재시도·완료 상태를 정의한다. */
public enum OutboxStatus {
    PENDING,
    RETRY,
    COMPLETED,
    FAILED
}
