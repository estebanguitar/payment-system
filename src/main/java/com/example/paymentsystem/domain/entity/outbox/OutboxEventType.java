package com.example.paymentsystem.domain.entity.outbox;

/** 아웃박스가 발행할 결제 도메인 이벤트 유형을 정의한다. */
public enum OutboxEventType {
    PAYMENT_REQUESTED,
    PAYMENT_CANCEL_REQUESTED
}
