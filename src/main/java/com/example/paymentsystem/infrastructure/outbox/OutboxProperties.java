package com.example.paymentsystem.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 아웃박스 고립 판단, 조회 묶음 및 재시도 정책 설정을 바인딩한다. */
@ConfigurationProperties(prefix = "payment.outbox")
public record OutboxProperties(
        boolean enabled,
        long fixedDelay,
        long orphanThreshold,
        int batchSize,
        int maxRetryCount) {

    /** 누락된 숫자 설정을 안전한 기본값으로 보정하고 범위를 검증한다. */
    public OutboxProperties {
        fixedDelay = fixedDelay == 0 ? 60_000 : fixedDelay;
        orphanThreshold = orphanThreshold == 0 ? 60_000 : orphanThreshold;
        batchSize = batchSize == 0 ? 50 : batchSize;
        maxRetryCount = maxRetryCount == 0 ? 5 : maxRetryCount;
        if (fixedDelay < 1 || orphanThreshold < 1 || batchSize < 1 || maxRetryCount < 1) {
            throw new IllegalArgumentException("아웃박스 설정값은 0보다 커야 합니다.");
        }
    }
}
