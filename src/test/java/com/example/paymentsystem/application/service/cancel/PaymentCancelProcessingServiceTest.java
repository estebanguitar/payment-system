package com.example.paymentsystem.application.service.cancel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.application.port.out.pg.PgCancelResult;
import com.example.paymentsystem.application.port.out.pg.PgResultStatus;
import com.example.paymentsystem.application.port.out.security.PayloadEncryptor;
import com.example.paymentsystem.domain.entity.cancel.CancelType;
import com.example.paymentsystem.domain.entity.cancel.PaymentCancel;
import com.example.paymentsystem.domain.entity.outbox.OutboxStatus;
import com.example.paymentsystem.domain.entity.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.entity.payment.PaymentFailureReason;
import com.example.paymentsystem.infrastructure.repository.cancel.PaymentCancelRepository;
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

/** 취소 결과 반영 서비스의 거절·시스템 실패 메서드를 독립 검증한다. */
class PaymentCancelProcessingServiceTest {
    private PaymentCancelProcessingService service;
    private PaymentCancel cancel;
    private PaymentOutbox outbox;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        PaymentCancelRepository cancelRepository = mock(PaymentCancelRepository.class);
        PaymentOutboxRepository outboxRepository = mock(PaymentOutboxRepository.class);
        ObjectProvider<PayloadEncryptor> provider = mock(ObjectProvider.class);
        PayloadEncryptor encryptor = mock(PayloadEncryptor.class);
        when(provider.getIfAvailable()).thenReturn(encryptor);
        when(encryptor.encrypt("raw")).thenReturn("encrypted");
        service = new PaymentCancelProcessingService(mock(PaymentRepository.class), cancelRepository,
                mock(WalletRepository.class), mock(WalletTransactionRepository.class),
                mock(PgResponseLogRepository.class), outboxRepository, provider);
        cancel = PaymentCancel.createPending(1L, "K", CancelType.FULL, 100, 100, LocalDateTime.now());
        ReflectionTestUtils.setField(cancel, "id", 2L);
        outbox = PaymentOutbox.createPaymentCancelRequested(1L, "K", "{}", LocalDateTime.now());
        when(cancelRepository.findById(2L)).thenReturn(Optional.of(cancel));
        when(outboxRepository.findByIdWithLock(3L)).thenReturn(Optional.of(outbox));
    }

    /** reject가 PG 취소 거절과 아웃박스 완료를 반영하는지 확인한다. */
    @Test
    void reject() {
        service.reject(1L, 2L, 3L, PgCancelResult.builder().status(PgResultStatus.REJECTED)
                .rawPayload("raw").responseCode("R").build());
        assertThat(cancel.getFailureReason()).isEqualTo(PaymentFailureReason.EXTERNAL_PAYMENT_REJECTED);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    /** failSystem이 PG 기술 실패를 취소 시스템 오류로 종료하는지 확인한다. */
    @Test
    void failSystem() {
        service.failSystem(2L, 3L);
        assertThat(cancel.getFailureReason()).isEqualTo(PaymentFailureReason.SYSTEM_ERROR);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }
}
