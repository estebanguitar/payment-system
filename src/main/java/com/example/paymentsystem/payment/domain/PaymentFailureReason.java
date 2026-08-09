package com.example.paymentsystem.payment.domain;

/** 결제 및 취소가 완료되지 못한 업무 원인을 구분한다. */
public enum PaymentFailureReason {
    INSUFFICIENT_BALANCE,
    SYSTEM_ERROR,
    EXTERNAL_PAYMENT_REJECTED
}
