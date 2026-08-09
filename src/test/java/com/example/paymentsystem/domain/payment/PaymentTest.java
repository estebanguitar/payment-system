package com.example.paymentsystem.domain.entity.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.domain.exception.InvalidCancelAmountException;
import com.example.paymentsystem.domain.exception.InvalidPaymentStateException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 결제 상태 전이, 실패 사유 및 누적 취소 규칙을 검증한다. */
class PaymentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    /** 신규 결제가 PENDING과 누적 취소액 0으로 생성되는지 확인한다. */
    @Test
    void createPendingPayment() {
        Payment payment = Payment.createPending("PAY-1", "CUST-001", 10_000, NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getAccumulatedCancelAmount()).isZero();
        assertThat(payment.matchesRequest("CUST-001", 10_000)).isTrue();
    }

    /** PENDING 결제가 완료 또는 명시적 실패 상태로만 전환되는지 확인한다. */
    @Test
    void completeOrFailPendingPayment() {
        Payment completed = Payment.createPending("PAY-1", "CUST-001", 10_000, NOW);
        Payment failed = Payment.createPending("PAY-2", "CUST-001", 10_000, NOW);

        completed.complete(NOW.plusSeconds(1));
        failed.fail(PaymentFailureReason.INSUFFICIENT_BALANCE, NOW.plusSeconds(1));

        assertThat(completed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(completed.getFailureReason()).isNull();
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo(PaymentFailureReason.INSUFFICIENT_BALANCE);
    }

    /** 반복 부분 취소 후 잔여 금액이 없으면 전액 취소로 전환되는지 확인한다. */
    @Test
    void applyPartialAndFullCancellation() {
        Payment payment = Payment.createPending("PAY-1", "CUST-001", 10_000, NOW);
        payment.complete(NOW);

        payment.applyCancellation(3_000, NOW.plusSeconds(1));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_CANCELED);
        assertThat(payment.remainingCancelableAmount()).isEqualTo(7_000);

        payment.applyCancellation(7_000, NOW.plusSeconds(2));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.remainingCancelableAmount()).isZero();
    }

    /** 허용되지 않은 상태와 잔여 금액 초과 취소를 거절하는지 확인한다. */
    @Test
    void rejectInvalidCancellation() {
        Payment pending = Payment.createPending("PAY-1", "CUST-001", 10_000, NOW);
        assertThatThrownBy(() -> pending.applyCancellation(1_000, NOW))
                .isInstanceOf(InvalidPaymentStateException.class);

        pending.complete(NOW);
        assertThatThrownBy(() -> pending.applyCancellation(10_001, NOW))
                .isInstanceOf(InvalidCancelAmountException.class);
    }

    /** 완료된 결제를 다시 완료 또는 실패 처리하지 못하게 하는지 확인한다. */
    @Test
    void rejectRepeatedTerminalTransition() {
        Payment payment = Payment.createPending("PAY-1", "CUST-001", 10_000, NOW);
        payment.complete(NOW);

        assertThatThrownBy(() -> payment.complete(NOW)).isInstanceOf(InvalidPaymentStateException.class);
        assertThatThrownBy(() -> payment.fail(PaymentFailureReason.SYSTEM_ERROR, NOW))
                .isInstanceOf(InvalidPaymentStateException.class);
    }
}
