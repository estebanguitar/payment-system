package com.example.paymentsystem.presentation.wallet.dto;

import com.example.paymentsystem.application.dto.wallet.WalletResult;
import lombok.Builder;
import lombok.Getter;

/** 외부에 노출할 지갑 충전 결과를 표현한다. */
@Getter
@Builder
public class WalletTopUpResponse {
    private final Long transactionId;
    private final String customerId;
    private final long amount;
    private final long balanceAfter;

    /** 애플리케이션 충전 결과를 API 응답으로 변환한다. */
    public static WalletTopUpResponse from(WalletResult.TopUp source) {
        return WalletTopUpResponse.builder().transactionId(source.getTransactionId())
                .customerId(source.getCustomerId()).amount(source.getAmount())
                .balanceAfter(source.getBalanceAfter()).build();
    }
}
