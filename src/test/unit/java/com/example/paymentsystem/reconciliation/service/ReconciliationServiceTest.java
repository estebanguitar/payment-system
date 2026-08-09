package com.example.paymentsystem.reconciliation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.domain.pg.PgOperationType;
import com.example.paymentsystem.domain.pg.PgResponseLog;
import com.example.paymentsystem.domain.reconciliation.ReconciliationBreak;
import com.example.paymentsystem.domain.reconciliation.ReconciliationBreakType;
import com.example.paymentsystem.domain.wallet.Wallet;
import com.example.paymentsystem.domain.wallet.WalletTransaction;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.repository.pg.PgResponseLogRepository;
import com.example.paymentsystem.repository.reconciliation.ReconciliationBreakRepository;
import com.example.paymentsystem.repository.wallet.WalletRepository;
import com.example.paymentsystem.repository.wallet.WalletTransactionRepository;
import com.example.paymentsystem.service.reconciliation.ReconciliationService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** 경량 대사가 결제·PG·지갑의 핵심 불일치를 기록하는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 12, 0);

    @Mock private PaymentRepository paymentRepository;
    @Mock private PgResponseLogRepository pgLogRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository transactionRepository;
    @Mock private ReconciliationBreakRepository breakRepository;

    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T03:00:00Z"), ZoneOffset.UTC);
        service = new ReconciliationService(paymentRepository, pgLogRepository, walletRepository,
                transactionRepository, breakRepository, clock);
    }

    /** 완료 결제 금액과 지갑 PAYMENT 합계가 다르면 불일치를 기록한다. */
    @Test
    void recordsPaymentWalletAmountMismatch() {
        Payment payment = completedPayment(1L, 10_000L);
        when(paymentRepository.findAll()).thenReturn(List.of(payment));
        when(walletRepository.findAll()).thenReturn(List.of());
        when(transactionRepository.findAllByPaymentIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(WalletTransaction.payment(2L, 9_000L, 1_000L, 1L, NOW)));
        when(pgLogRepository.findAllByPaymentIdAndOperationTypeOrderByReceivedAtAsc(
                1L, PgOperationType.PAYMENT_APPROVAL))
                .thenReturn(List.of(PgResponseLog.create(1L, PgOperationType.PAYMENT_APPROVAL,
                        "PG-1", "0000", "encrypted", NOW)));

        service.reconcile();

        ArgumentCaptor<ReconciliationBreak> captor = ArgumentCaptor.forClass(ReconciliationBreak.class);
        verify(breakRepository).save(captor.capture());
        assertThat(captor.getValue().getBreakType()).isEqualTo(ReconciliationBreakType.PAYMENT_WALLET_AMOUNT);
    }

    /** 완료 결제에 승인 로그가 없으면 PG 로그 누락을 기록한다. */
    @Test
    void recordsMissingPgLog() {
        Payment payment = completedPayment(1L, 10_000L);
        when(paymentRepository.findAll()).thenReturn(List.of(payment));
        when(walletRepository.findAll()).thenReturn(List.of());
        when(transactionRepository.findAllByPaymentIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(WalletTransaction.payment(2L, 10_000L, 0L, 1L, NOW)));
        when(pgLogRepository.findAllByPaymentIdAndOperationTypeOrderByReceivedAtAsc(
                1L, PgOperationType.PAYMENT_APPROVAL)).thenReturn(List.of());

        service.reconcile();

        ArgumentCaptor<ReconciliationBreak> captor = ArgumentCaptor.forClass(ReconciliationBreak.class);
        verify(breakRepository).save(captor.capture());
        assertThat(captor.getValue().getBreakType()).isEqualTo(ReconciliationBreakType.PAYMENT_PG_LOG_MISSING);
    }

    /** 지갑 입출금 순합계와 현재 잔고가 다르면 잔고 불일치를 기록한다. */
    @Test
    void recordsWalletBalanceMismatch() {
        Wallet wallet = Wallet.create("customer-1", NOW);
        ReflectionTestUtils.setField(wallet, "id", 2L);
        wallet.increase(5_000L, NOW);
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(walletRepository.findAll()).thenReturn(List.of(wallet));
        when(transactionRepository.findAllByWalletIdOrderByCreatedAtAsc(2L))
                .thenReturn(List.of(WalletTransaction.topUp(2L, 4_000L, 4_000L, "topup-1", NOW)));

        service.reconcile();

        ArgumentCaptor<ReconciliationBreak> captor = ArgumentCaptor.forClass(ReconciliationBreak.class);
        verify(breakRepository).save(captor.capture());
        assertThat(captor.getValue().getBreakType()).isEqualTo(ReconciliationBreakType.WALLET_BALANCE);
    }

    /** 같은 불일치는 고유 키로 한 번만 기록한다. */
    @Test
    void skipsExistingBreak() {
        Wallet wallet = Wallet.create("customer-1", NOW);
        ReflectionTestUtils.setField(wallet, "id", 2L);
        wallet.increase(5_000L, NOW);
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(walletRepository.findAll()).thenReturn(List.of(wallet));
        when(transactionRepository.findAllByWalletIdOrderByCreatedAtAsc(2L)).thenReturn(List.of());
        when(breakRepository.existsByBreakKey(any())).thenReturn(true);

        service.reconcile();

        verify(breakRepository, never()).save(any());
    }

    private static Payment completedPayment(Long id, long amount) {
        Payment payment = Payment.createPending("payment-" + id, "customer-1", amount, NOW);
        ReflectionTestUtils.setField(payment, "id", id);
        payment.complete(NOW);
        return payment;
    }
}
