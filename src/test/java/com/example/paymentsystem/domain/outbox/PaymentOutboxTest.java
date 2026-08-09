package com.example.paymentsystem.domain.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.common.exception.InvalidPaymentStateException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 아웃박스 생성, 재시도 및 최종 상태 전이를 검증한다. */
class PaymentOutboxTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    /** 신규 결제 요청 이벤트가 PENDING 및 재시도 0으로 생성되는지 확인한다. */
    @Test
    void createInitialOutbox() {
        PaymentOutbox outbox = PaymentOutbox.createPaymentRequested(1L, "PAY-1", "{}", NOW);

        assertThat(outbox.getEventType()).isEqualTo(OutboxEventType.PAYMENT_REQUESTED);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isZero();
    }

    /** 발행 실패를 기록하면 RETRY 상태로 전환되는지 확인한다. */
    @Test
    void recordRetryableFailure() {
        PaymentOutbox outbox = PaymentOutbox.createPaymentRequested(1L, "PAY-1", "{}", NOW);

        outbox.recordFailure(NOW.plusSeconds(1));

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.RETRY);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
    }

    /** 최종 상태로 전환된 이벤트는 다시 변경할 수 없는지 확인한다. */
    @Test
    void rejectTransitionAfterPublished() {
        PaymentOutbox outbox = PaymentOutbox.createPaymentRequested(1L, "PAY-1", "{}", NOW);
        outbox.markCompleted(NOW.plusSeconds(1));

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.COMPLETED);
        assertThatThrownBy(() -> outbox.recordFailure(NOW.plusSeconds(2)))
                .isInstanceOf(InvalidPaymentStateException.class);
    }
}
