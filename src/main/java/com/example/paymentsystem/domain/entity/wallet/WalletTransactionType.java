package com.example.paymentsystem.domain.entity.wallet;

/** 지갑 잔액을 변경한 업무 원인을 구분한다. */
public enum WalletTransactionType {
    TOP_UP,
    PAYMENT,
    REFUND
}
