package com.example.paymentsystem.payment.domain;

/** 결제 접수부터 완료·실패·취소까지의 원장 상태를 정의한다. */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    PARTIALLY_CANCELED,
    CANCELED,
    FAILED
}
