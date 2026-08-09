package com.example.paymentsystem.payment.application.dto.query;

import com.example.paymentsystem.wallet.domain.WalletTransaction;
import com.example.paymentsystem.wallet.domain.WalletTransactionType;
import java.time.LocalDateTime;

/** 운영 조회에 필요한 지갑 거래 참조 정보만 전달한다. */
public record WalletTransactionView(Long transactionId, WalletTransactionType type, long amount,
                                    long balanceAfter, Long paymentId, Long cancelId,
                                    LocalDateTime createdAt) {
    /** 지갑 거래 엔티티를 안전한 조회 DTO로 변환한다. */
    public static WalletTransactionView from(WalletTransaction transaction) {
        return new WalletTransactionView(transaction.getId(), transaction.getTransactionType(),
                transaction.getAmount(), transaction.getBalanceAfter(), transaction.getPaymentId(),
                transaction.getCancelId(), transaction.getCreatedAt());
    }
}
