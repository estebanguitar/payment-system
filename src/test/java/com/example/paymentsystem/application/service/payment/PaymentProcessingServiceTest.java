package com.example.paymentsystem.application.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.application.port.out.pg.PgApprovalResult;
import com.example.paymentsystem.application.port.out.pg.PgResultStatus;
import com.example.paymentsystem.application.port.out.security.PayloadEncryptor;
import com.example.paymentsystem.shared.domain.exception.InsufficientBalanceException;
import com.example.paymentsystem.domain.entity.outbox.OutboxStatus;
import com.example.paymentsystem.domain.entity.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.entity.payment.Payment;
import com.example.paymentsystem.domain.entity.payment.PaymentFailureReason;
import com.example.paymentsystem.domain.entity.wallet.Wallet;
import com.example.paymentsystem.infrastructure.repository.outbox.PaymentOutboxRepository;
import com.example.paymentsystem.infrastructure.repository.payment.PaymentRepository;
import com.example.paymentsystem.infrastructure.repository.pg.PgResponseLogRepository;
import com.example.paymentsystem.infrastructure.repository.wallet.WalletRepository;
import com.example.paymentsystem.infrastructure.repository.wallet.WalletTransactionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

/** 결제 결과 반영 서비스의 승인 외 최종 실패 메서드를 독립 검증한다. */
class PaymentProcessingServiceTest {
    private PaymentRepository paymentRepository;
    private PaymentOutboxRepository outboxRepository;
    private WalletRepository walletRepository;
    private PgResponseLogRepository pgLogRepository;
    private PaymentProcessingService service;
    private Payment payment;
    private PaymentOutbox outbox;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        outboxRepository = mock(PaymentOutboxRepository.class);
        walletRepository = mock(WalletRepository.class);
        pgLogRepository = mock(PgResponseLogRepository.class);
        ObjectProvider<PayloadEncryptor> provider = mock(ObjectProvider.class);
        PayloadEncryptor encryptor = mock(PayloadEncryptor.class);
        when(provider.getIfAvailable()).thenReturn(encryptor);
        when(encryptor.encrypt("raw")).thenReturn("encrypted");
        service = new PaymentProcessingService(paymentRepository, walletRepository,
                mock(WalletTransactionRepository.class), pgLogRepository,
                outboxRepository, provider);
        payment = Payment.createPending("K", "C", 100, LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", 1L);
        outbox = PaymentOutbox.createPaymentRequested(1L, "K", "{}", LocalDateTime.now());
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(outboxRepository.findByIdWithLock(2L)).thenReturn(Optional.of(outbox));
    }

    /** approve가 PG 승인 후 잔액 부족을 최종 실패로 기록하고 로그와 아웃박스를 완료하는지 확인한다. */
    @Test
    void approveWithInsufficientBalance() {
        Wallet wallet = mock(Wallet.class);
        when(wallet.decrease(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenThrow(new InsufficientBalanceException());
        when(walletRepository.findByCustomerIdWithLock("C")).thenReturn(Optional.of(wallet));

        service.approve(1L, 2L, PgApprovalResult.builder().status(PgResultStatus.APPROVED)
                .pgTransactionId("PG-1").responseCode("0000").rawPayload("raw").build());

        assertThat(payment.getFailureReason()).isEqualTo(PaymentFailureReason.INSUFFICIENT_BALANCE);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        verify(pgLogRepository).save(org.mockito.ArgumentMatchers.any());
    }

    /** reject가 PG 거절을 결제 실패와 PUBLISHED 아웃박스로 반영하는지 확인한다. */
    @Test
    void reject() {
        service.reject(1L, 2L, PgApprovalResult.builder().status(PgResultStatus.REJECTED)
                .rawPayload("raw").responseCode("R").build());
        assertThat(payment.getFailureReason()).isEqualTo(PaymentFailureReason.EXTERNAL_PAYMENT_REJECTED);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    /** failSystem이 PG 기술 실패를 시스템 오류로 최종 종료하는지 확인한다. */
    @Test
    void failSystem() {
        service.failSystem(1L, 2L);
        assertThat(payment.getFailureReason()).isEqualTo(PaymentFailureReason.SYSTEM_ERROR);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }
}
