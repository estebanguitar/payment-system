package com.example.paymentsystem.domain.pg;

/** PG 응답이 결제 승인과 취소 중 어느 요청에서 발생했는지 구분한다. */
public enum PgOperationType {
    PAYMENT_APPROVAL,
    PAYMENT_CANCEL
}
