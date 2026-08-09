package com.example.paymentsystem.presentation.wallet.dto;

import com.example.paymentsystem.application.dto.wallet.WalletCommand;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 지갑 충전 금액을 검증하고 애플리케이션 명령으로 변환한다. */
@Getter
@NoArgsConstructor
public class TopUpWalletRequest {
    @Positive
    private long amount;

    /** 경로와 헤더 값을 결합해 충전 명령을 생성한다. */
    public WalletCommand.TopUp toCommand(String customerId, String idempotencyKey) {
        return WalletCommand.TopUp.builder().customerId(customerId).idempotencyKey(idempotencyKey)
                .amount(amount).build();
    }
}
