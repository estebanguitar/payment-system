package com.example.paymentsystem.presentation.query.dto;

import com.example.paymentsystem.application.dto.query.WalletTransactionView;
import com.example.paymentsystem.wallet.domain.WalletTransactionType;
import java.time.LocalDateTime;

/** 운영 조회 응답에 포함되는 지갑 거래 항목이다. */
public record WalletTransactionItemResponse(Long transactionId, WalletTransactionType type, long amount,
        long balanceAfter, Long paymentId, Long cancelId, LocalDateTime createdAt) {
    /** 애플리케이션 지갑 거래 조회 결과를 API 항목으로 변환한다. */
    public static WalletTransactionItemResponse from(WalletTransactionView source) {
        return new WalletTransactionItemResponse(source.transactionId(), source.type(), source.amount(),
                source.balanceAfter(), source.paymentId(), source.cancelId(), source.createdAt());
    }
}
