package com.example.paymentsystem.application.dto.wallet;

import com.example.paymentsystem.domain.entity.wallet.Wallet;
import com.example.paymentsystem.domain.entity.wallet.WalletTransaction;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** Presentation과 분리된 지갑 조회·충전 결과를 제공한다. */
public final class WalletResult {

    private WalletResult() {
    }

    /** 지갑 식별자와 현재 잔액을 반환하는 결과다. */
    @Getter
    @Builder
    public static class Balance {
        private final Long walletId;
        private final String customerId;
        private final long balance;
        private final LocalDateTime createdAt;

        /** Wallet 엔티티를 읽기 결과로 변환한다. */
        public static Balance from(Wallet wallet) {
            return Balance.builder().walletId(wallet.getId()).customerId(wallet.getCustomerId())
                    .balance(wallet.getBalance()).createdAt(wallet.getCreatedAt()).build();
        }
    }

    /** 충전 거래와 반영 후 잔액을 반환하는 결과다. */
    @Getter
    @Builder
    public static class TopUp {
        private final Long transactionId;
        private final String customerId;
        private final long amount;
        private final long balanceAfter;

        /** 충전 거래 엔티티를 Application 결과로 변환한다. */
        public static TopUp from(String customerId, WalletTransaction transaction) {
            return TopUp.builder().transactionId(transaction.getId()).customerId(customerId)
                    .amount(transaction.getAmount()).balanceAfter(transaction.getBalanceAfter()).build();
        }
    }
}
