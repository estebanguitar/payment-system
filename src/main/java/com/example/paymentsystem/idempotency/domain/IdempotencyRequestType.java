package com.example.paymentsystem.idempotency.domain;

/** 시스템 전체 멱등키를 사용하는 업무 요청 유형을 구분한다. */
public enum IdempotencyRequestType {
    PAYMENT,
    PAYMENT_CANCEL,
    WALLET_TOP_UP
}
