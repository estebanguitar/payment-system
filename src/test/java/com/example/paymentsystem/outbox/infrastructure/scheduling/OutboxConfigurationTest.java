package com.example.paymentsystem.outbox.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** 기본 설정에서 미완성 Application 복구 흐름이 실행되지 않는지 검증한다. */
@SpringBootTest
class OutboxConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    /** outbox.enabled=false이면 스케줄러 Bean을 생성하지 않는지 확인한다. */
    @Test
    void disableSchedulerByDefault() {
        assertThat(applicationContext.getBeansOfType(OutboxScheduler.class)).isEmpty();
    }
}
