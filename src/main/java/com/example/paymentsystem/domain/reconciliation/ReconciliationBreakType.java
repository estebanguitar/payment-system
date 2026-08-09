package com.example.paymentsystem.domain.reconciliation;

/** 경량 대사에서 탐지하는 원장·PG·지갑 불일치 유형이다. */
public enum ReconciliationBreakType {
    PAYMENT_WALLET_AMOUNT,
    PAYMENT_PG_STATUS,
    PAYMENT_PG_LOG_MISSING,
    WALLET_BALANCE
}
