package com.example.paymentsystem.outbox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.cancellation.application.event.PaymentCancelEventListener;
import com.example.paymentsystem.payment.application.event.PaymentEventListener;
import com.example.paymentsystem.outbox.domain.OutboxStatus;
import com.example.paymentsystem.outbox.domain.PaymentOutbox;
import com.example.paymentsystem.outbox.infrastructure.scheduling.OutboxProperties;
import com.example.paymentsystem.outbox.infrastructure.repository.PaymentOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** 아웃박스 복구와 재시도 상태 갱신 메서드의 모든 이벤트 유형을 검증한다. */
class OutboxServiceTest {
    /** recover가 결제·취소 INIT 이벤트를 해당 리스너로 전달하고 완료 이벤트는 무시하는지 확인한다. */
    @Test
    void recoverPaymentCancelAndIgnorePublished() {
        PaymentOutboxRepository repository = mock(PaymentOutboxRepository.class);
        PaymentEventListener paymentListener = mock(PaymentEventListener.class);
        PaymentCancelEventListener cancelListener = mock(PaymentCancelEventListener.class);
        OutboxQueryService queryService = mock(OutboxQueryService.class);
        OutboxRecoveryService service = new OutboxRecoveryService(queryService, paymentListener, cancelListener,
                mock(OutboxStatusService.class), new ObjectMapper());
        PaymentOutbox payment = PaymentOutbox.createPaymentRequested(1L, "K1", "{\"paymentId\":1}", LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", 10L);
        PaymentOutbox cancel = PaymentOutbox.createPaymentCancelRequested(1L, "K2",
                "{\"paymentId\":1,\"cancelId\":2}", LocalDateTime.now());
        ReflectionTestUtils.setField(cancel, "id", 11L);
        PaymentOutbox published = PaymentOutbox.createPaymentRequested(1L, "K3", "{\"paymentId\":1}", LocalDateTime.now());
        published.markPublished(LocalDateTime.now());
        when(queryService.get(10L)).thenReturn(payment);
        when(queryService.get(11L)).thenReturn(cancel);
        when(queryService.get(12L)).thenReturn(published);
        service.recover(10L); service.recover(11L); service.recover(12L);
        verify(paymentListener).handle(org.mockito.ArgumentMatchers.any());
        verify(cancelListener).handle(org.mockito.ArgumentMatchers.any());
    }

    /** recover가 잘못된 payload를 시스템 오류로 처리하는지 확인한다. */
    @Test
    void rejectInvalidPayload() {
        PaymentOutboxRepository repository = mock(PaymentOutboxRepository.class);
        PaymentOutbox outbox = PaymentOutbox.createPaymentRequested(1L, "K", "{}", LocalDateTime.now());
        OutboxQueryService queryService = mock(OutboxQueryService.class);
        when(queryService.get(1L)).thenReturn(outbox);
        OutboxStatusService status = mock(OutboxStatusService.class);
        OutboxRecoveryService service = new OutboxRecoveryService(queryService, mock(PaymentEventListener.class),
                mock(PaymentCancelEventListener.class), status, new ObjectMapper());
        assertThatThrownBy(() -> service.recover(1L)).isInstanceOf(RuntimeException.class);
        verify(status).recordFailure(1L);
    }

    /** OutboxQueryService.get이 아웃박스를 반환하고 미존재 ID를 시스템 오류로 변환하는지 확인한다. */
    @Test
    void queryOutbox() {
        PaymentOutboxRepository repository = mock(PaymentOutboxRepository.class);
        PaymentOutbox outbox = PaymentOutbox.createPaymentRequested(1L, "K", "{}", LocalDateTime.now());
        when(repository.findById(1L)).thenReturn(Optional.of(outbox));
        OutboxQueryService service = new OutboxQueryService(repository);
        assertThat(service.get(1L)).isSameAs(outbox);
        assertThatThrownBy(() -> service.get(2L)).isInstanceOf(RuntimeException.class);
    }

    /** recordFailure가 횟수를 증가시키고 한도 도달 시 FAILED로 전환하는지 확인한다. */
    @Test
    void recordFailureAndMarkFailed() {
        PaymentOutboxRepository repository = mock(PaymentOutboxRepository.class);
        PaymentOutbox outbox = PaymentOutbox.createPaymentRequested(1L, "K", "{}", LocalDateTime.now());
        when(repository.findByIdWithLock(1L)).thenReturn(Optional.of(outbox));
        OutboxStatusService service = new OutboxStatusService(repository,
                new OutboxProperties(false, 1, 1, 1, 1));
        service.recordFailure(1L);
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
    }
}
