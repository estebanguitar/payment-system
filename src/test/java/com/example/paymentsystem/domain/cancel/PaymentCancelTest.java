package com.example.paymentsystem.domain.entity.cancel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.shared.domain.exception.InvalidCancelAmountException;
import com.example.paymentsystem.shared.domain.exception.InvalidPaymentStateException;
import com.example.paymentsystem.domain.entity.payment.PaymentFailureReason;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 취소 유형별 금액 검증과 취소 상태 전이를 검증한다. */
class PaymentCancelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    /** 전액 취소는 남은 취소 가능 금액 전체로 생성되는지 확인한다. */
    @Test
    void createFullCancellation() {
        PaymentCancel cancel = PaymentCancel.createPending(
                1L, "CANCEL-1", CancelType.FULL, 10_000, 10_000, NOW);

        assertThat(cancel.getStatus()).isEqualTo(CancelStatus.PENDING);
        assertThat(cancel.getAmount()).isEqualTo(10_000);
    }

    /** 부분 취소는 남은 취소 가능 금액보다 작은 경우에만 허용하는지 확인한다. */
    @Test
    void validatePartialCancellationAmount() {
        PaymentCancel cancel = PaymentCancel.createPending(
                1L, "CANCEL-1", CancelType.PARTIAL, 3_000, 10_000, NOW);

        assertThat(cancel.getCancelType()).isEqualTo(CancelType.PARTIAL);
        assertThatThrownBy(() -> PaymentCancel.createPending(
                1L, "CANCEL-2", CancelType.PARTIAL, 10_000, 10_000, NOW))
                .isInstanceOf(InvalidCancelAmountException.class);
    }

    /** 외부 PG 거래 식별자와 함께 취소 완료 상태로 전환되는지 확인한다. */
    @Test
    void completeCancellation() {
        PaymentCancel cancel = PaymentCancel.createPending(
                1L, "CANCEL-1", CancelType.FULL, 10_000, 10_000, NOW);

        cancel.complete("PG-CANCEL-1", NOW.plusSeconds(1));

        assertThat(cancel.getStatus()).isEqualTo(CancelStatus.COMPLETED);
        assertThat(cancel.getPgCancelTransactionId()).isEqualTo("PG-CANCEL-1");
        assertThat(cancel.getCompletedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    /** 취소 실패가 해당 취소 시도에만 사유로 기록되는지 확인한다. */
    @Test
    void failCancellation() {
        PaymentCancel cancel = PaymentCancel.createPending(
                1L, "CANCEL-1", CancelType.FULL, 10_000, 10_000, NOW);

        cancel.fail(PaymentFailureReason.SYSTEM_ERROR, NOW.plusSeconds(1));

        assertThat(cancel.getStatus()).isEqualTo(CancelStatus.FAILED);
        assertThat(cancel.getFailureReason()).isEqualTo(PaymentFailureReason.SYSTEM_ERROR);
        assertThatThrownBy(() -> cancel.fail(PaymentFailureReason.SYSTEM_ERROR, NOW))
                .isInstanceOf(InvalidPaymentStateException.class);
    }
}
