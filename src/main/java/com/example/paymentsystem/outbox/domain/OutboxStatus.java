package com.example.paymentsystem.outbox.domain;

/** 아웃박스 이벤트의 발행 및 최종 실패 상태를 정의한다. */
public enum OutboxStatus {
    INIT,
    PUBLISHED,
    FAILED
}
