package com.example.paymentsystem.outbox.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 검증된 Outbox 실행 주기를 제공하는 설정 어댑터다. */
@Component
@RequiredArgsConstructor
public class OutboxScheduleDelayProvider {

    private final OutboxProperties properties;

    /** 설정에서 검증된 실행 간격을 밀리초로 반환한다. */
    public long fixedDelay() {
        return properties.fixedDelay();
    }
}
