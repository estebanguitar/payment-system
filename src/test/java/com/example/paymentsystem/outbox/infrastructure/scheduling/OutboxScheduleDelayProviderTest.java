package com.example.paymentsystem.outbox.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 스케줄 실행 간격 제공자가 검증된 설정값만 노출하는지 확인한다. */
class OutboxScheduleDelayProviderTest {

    /** OutboxProperties의 fixed delay 값을 변경 없이 반환하는지 확인한다. */
    @Test
    void provideValidatedFixedDelay() {
        OutboxScheduleDelayProvider provider = new OutboxScheduleDelayProvider(
                new OutboxProperties(true, 12_345, 60_000, 50, 5));

        assertThat(provider.fixedDelay()).isEqualTo(12_345);
    }
}
