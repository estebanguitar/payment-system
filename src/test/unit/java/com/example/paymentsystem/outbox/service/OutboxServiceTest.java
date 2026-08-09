package com.example.paymentsystem.service.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.domain.outbox.OutboxStatus;
import com.example.paymentsystem.domain.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.integration.pg.pg.PgApprovalResult;
import com.example.paymentsystem.integration.pg.pg.PgClient;
import com.example.paymentsystem.integration.pg.pg.PgResultStatus;
import com.example.paymentsystem.repository.outbox.PaymentOutboxRepository;
import com.example.paymentsystem.repository.payment.PaymentCancelRepository;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.scheduler.outbox.OutboxProperties;
import com.example.paymentsystem.service.payment.PaymentCancelProcessingService;
import com.example.paymentsystem.service.payment.PaymentProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 공통 OutboxProcessor와 재시도 상태 변경을 검증한다. */
class OutboxServiceTest {
    /** 결제 이벤트의 PG 승인 결과를 결제 처리 서비스로 전달하는지 확인한다. */
    @Test
    void processApprovedPayment() {
        PaymentOutboxRepository outboxes = mock(PaymentOutboxRepository.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        PgClient pgClient = mock(PgClient.class);
        PaymentProcessingService processing = mock(PaymentProcessingService.class);
        OutboxStatusService status = mock(OutboxStatusService.class);
        PaymentOutbox outbox = PaymentOutbox.createPaymentRequested(
                1L, "K", "{\"paymentId\":1}", LocalDateTime.now());
        ReflectionTestUtils.setField(outbox, "id", 2L);
        Payment payment = Payment.createPending("K", "C", 100, LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", 1L);
        PgApprovalResult result = PgApprovalResult.builder().status(PgResultStatus.APPROVED).build();
        when(outboxes.findById(2L)).thenReturn(Optional.of(outbox));
        when(payments.findById(1L)).thenReturn(Optional.of(payment));
        when(pgClient.approve(org.mockito.ArgumentMatchers.any())).thenReturn(result);
        OutboxProcessor processor = new OutboxProcessor(outboxes, payments, mock(PaymentCancelRepository.class),
                pgClient, processing, mock(PaymentCancelProcessingService.class), status, new ObjectMapper());

        processor.process(2L);

        verify(processing).approve(1L, 2L, result);
    }

    /** 손상된 payload는 실패 횟수를 기록하고 오류를 반환하는지 확인한다. */
    @Test
    void rejectInvalidPayload() {
        PaymentOutboxRepository outboxes = mock(PaymentOutboxRepository.class);
        PaymentOutbox outbox = PaymentOutbox.createPaymentRequested(1L, "K", "{}", LocalDateTime.now());
        when(outboxes.findById(1L)).thenReturn(Optional.of(outbox));
        OutboxStatusService status = mock(OutboxStatusService.class);
        OutboxProcessor processor = new OutboxProcessor(outboxes, mock(PaymentRepository.class),
                mock(PaymentCancelRepository.class), mock(PgClient.class), mock(PaymentProcessingService.class),
                mock(PaymentCancelProcessingService.class), status, new ObjectMapper());

        assertThatThrownBy(() -> processor.process(1L)).isInstanceOf(RuntimeException.class);
        verify(status).recordFailure(1L);
    }

    /** 재시도 한도 도달 시 Outbox를 FAILED로 전환하는지 확인한다. */
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
