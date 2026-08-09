package com.example.paymentsystem.wallet.presentation.dto;

import com.example.paymentsystem.wallet.application.dto.WalletResult;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 외부에 노출할 지갑 잔액 정보를 표현한다. */
@Getter
@Builder
public class WalletResponse {
    private final Long walletId;
    private final String customerId;
    private final long balance;
    private final LocalDateTime createdAt;

    /** 애플리케이션 조회 결과를 API 응답으로 변환한다. */
    public static WalletResponse from(WalletResult.Balance source) {
        return WalletResponse.builder().walletId(source.getWalletId()).customerId(source.getCustomerId())
                .balance(source.getBalance()).createdAt(source.getCreatedAt()).build();
    }
}
