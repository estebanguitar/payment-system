package com.example.paymentsystem.application.dto.wallet;

import lombok.Builder;
import lombok.Getter;

/** 지갑 Application 유스케이스 입력 명령을 정의한다. */
public final class WalletCommand {

    private WalletCommand() {
    }

    /** 멱등키가 포함된 지갑 충전 명령이다. */
    @Getter
    @Builder
    public static class TopUp {
        private final String customerId;
        private final long amount;
        private final String idempotencyKey;
    }
}
