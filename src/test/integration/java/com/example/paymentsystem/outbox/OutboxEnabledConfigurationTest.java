package com.example.paymentsystem.scheduler.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentsystem.service.outbox.OutboxProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 아웃박스 활성 환경에서 스케줄러의 설정 참조와 Bean 구성을 검증한다. */
@SpringBootTest(properties = {
        "payment.outbox.enabled=true",
        "payment.outbox.fixed-delay=12345"
})
class OutboxEnabledConfigurationTest {

    @MockitoBean
    private OutboxProcessor recoveryPort;

    @Autowired
    private OutboxScheduleDelayProvider delayProvider;

    /** 활성 스케줄러가 검증된 설정값을 SpEL 실행 간격으로 해석해 기동하는지 확인한다. */
    @Test
    void startSchedulerWithValidatedDelay() {
        assertThat(delayProvider.fixedDelay()).isEqualTo(12_345);
    }
}
