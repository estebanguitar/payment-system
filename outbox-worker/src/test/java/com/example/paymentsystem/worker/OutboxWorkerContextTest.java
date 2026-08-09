package com.example.paymentsystem.worker;

import com.example.paymentsystem.payment.presentation.PaymentController;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/** API 프로세스 없이 Worker가 독립적으로 기동되는 실행 경계를 검증한다. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:worker-test;DB_CLOSE_DELAY=-1",
        "payment.outbox.fixed-delay=3600000"
})
class OutboxWorkerContextTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxClaimService claimService;

    /** Worker가 스케줄러를 제공하되 API Controller는 노출하지 않는지 확인한다. */
    @Test
    void startWithoutApiPresentation() {
        Assertions.assertThat(context.getBean(OutboxWorkerScheduler.class)).isNotNull();
        Assertions.assertThatThrownBy(() -> context.getBean(PaymentController.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    /** 두 Worker가 같은 이벤트를 동시에 유효 선점할 수 없는지 확인한다. */
    @Test
    void claimEventOnlyOnce() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                insert into payment(id, idempotency_key, customer_id, amount, status,
                                    accumulated_cancel_amount, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, 100L, "WORKER-PAY", "CUST-001", 1000L, "PENDING", 0L, now, now);
        jdbcTemplate.update("""
                insert into payment_outbox(id, payment_id, idempotency_key, event_type, payload,
                                           status, retry_count, created_at, updated_at, next_attempt_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, 100L, 100L, "WORKER-OUTBOX", "PAYMENT_REQUESTED", "{\"paymentId\":100}",
                "INIT", 0, now, now, now);

        boolean first = claimService.claim(100L, "worker-a", now, now.plusMinutes(1));
        boolean second = claimService.claim(100L, "worker-b", now, now.plusMinutes(1));

        Assertions.assertThat(first).isTrue();
        Assertions.assertThat(second).isFalse();
    }
}
