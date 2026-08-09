package com.example.paymentsystem.domain.entity.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 지갑 거래 유형별 참조 식별자 규칙을 검증한다. */
class WalletTransactionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    /** 충전 거래에는 멱등키만 연결되는지 확인한다. */
    @Test
    void createTopUpTransaction() {
        WalletTransaction transaction = WalletTransaction.topUp(1L, 1_000, 2_000, "TOPUP-1", NOW);

        assertThat(transaction.getTransactionType()).isEqualTo(WalletTransactionType.TOP_UP);
        assertThat(transaction.getIdempotencyKey()).isEqualTo("TOPUP-1");
        assertThat(transaction.getPaymentId()).isNull();
        assertThat(transaction.getCancelId()).isNull();
    }

    /** 결제와 환불 거래에 필수 참조가 설정되는지 확인한다. */
    @Test
    void createPaymentAndRefundTransactions() {
        WalletTransaction payment = WalletTransaction.payment(1L, 1_000, 1_000, 10L, NOW);
        WalletTransaction refund = WalletTransaction.refund(1L, 500, 1_500, 10L, 20L, NOW);

        assertThat(payment.getTransactionType()).isEqualTo(WalletTransactionType.PAYMENT);
        assertThat(refund.getTransactionType()).isEqualTo(WalletTransactionType.REFUND);
        assertThat(refund.getCancelId()).isEqualTo(20L);
    }

    /** 거래 유형별 필수 식별자가 없으면 생성을 거절하는지 확인한다. */
    @Test
    void rejectMissingReferences() {
        assertThatThrownBy(() -> WalletTransaction.topUp(1L, 1_000, 1_000, " ", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> WalletTransaction.payment(1L, 1_000, 0, null, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> WalletTransaction.refund(1L, 1_000, 1_000, 10L, null, NOW))
                .isInstanceOf(DomainException.class);
    }
}
