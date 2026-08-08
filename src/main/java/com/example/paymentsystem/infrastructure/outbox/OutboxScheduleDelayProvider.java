package com.example.paymentsystem.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 검증된 아웃박스 설정에서 스케줄 실행 간격을 제공한다. */
@Component
@RequiredArgsConstructor
public class OutboxScheduleDelayProvider {

    private final OutboxProperties properties;

    /** 기본값 보정과 범위 검증을 통과한 fixed delay를 반환한다. */
    public long fixedDelay() {
        return properties.fixedDelay();
    }
}
