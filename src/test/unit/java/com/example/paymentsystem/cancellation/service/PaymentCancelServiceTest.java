package com.example.paymentsystem.service.cancellation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.dto.cancellation.PaymentCancelCommand;
import com.example.paymentsystem.dto.cancellation.PaymentCancelResult;
import com.example.paymentsystem.service.outbox.PaymentCancelCreatedEvent;
import com.example.paymentsystem.service.outbox.PaymentCancelEventListener;
import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.integration.pg.pg.PgCancelResult;
import com.example.paymentsystem.integration.pg.pg.PgClient;
import com.example.paymentsystem.integration.pg.pg.PgClientException;
import com.example.paymentsystem.integration.pg.pg.PgErrorType;
import com.example.paymentsystem.integration.pg.pg.PgResultStatus;
import com.example.paymentsystem.service.idempotency.IdempotencyConflictResolver;
import com.example.paymentsystem.service.idempotency.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.domain.cancellation.CancelType;
import com.example.paymentsystem.domain.cancellation.PaymentCancel;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.repository.cancellation.PaymentCancelRepository;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/** 취소 Facade·결과 조회·이벤트 리스너의 공개 메서드별 분기를 검증한다. */
class PaymentCancelServiceTest {
    /** cancel이 최초 생성과 멱등 충돌 재조회 결과를 각각 반환하는지 확인한다. */
    @Test
    void cancelAndResolveDuplicate() {
        PaymentCancelCreationService creation = mock(PaymentCancelCreationService.class);
        PaymentCancelResultService resultService = mock(PaymentCancelResultService.class);
        IdempotencyConflictResolver resolver = mock(IdempotencyConflictResolver.class);
        PaymentCancelCommand command = command();
        when(creation.createPending(command)).thenReturn(1L);
        when(resultService.get(1L)).thenReturn(PaymentCancelResult.builder().cancelId(1L).build());
        PaymentCancelFacade facade = new PaymentCancelFacade(creation, resultService, resolver,
                new IdempotencyFingerprintGenerator());
        assertThat(facade.cancel(command).getCancelId()).isEqualTo(1L);

        when(creation.createPending(command)).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(resolver.resolve(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(2L);
        when(resultService.get(2L)).thenReturn(PaymentCancelResult.builder().cancelId(2L).build());
        assertThat(facade.cancel(command).getCancelId()).isEqualTo(2L);
    }

    /** get과 getEntity가 취소를 조회하고 미존재 취소를 오류 처리하는지 확인한다. */
    @Test
    void getCancelResultAndEntity() {
        PaymentCancelRepository cancelRepository = mock(PaymentCancelRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        Payment payment = Payment.createPending("PK", "C", 100, LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", 1L);
        PaymentCancel cancel = PaymentCancel.createPending(1L, "CK", CancelType.FULL, 100, 100,
                LocalDateTime.now());
        ReflectionTestUtils.setField(cancel, "id", 2L);
        when(cancelRepository.findById(2L)).thenReturn(Optional.of(cancel));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentCancelResultService service = new PaymentCancelResultService(cancelRepository, paymentRepository);
        assertThat(service.get(2L).getCancelId()).isEqualTo(2L);
        assertThat(service.getEntity(2L)).isSameAs(cancel);
        assertThatThrownBy(() -> service.getEntity(3L)).extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.CANCEL_NOT_FOUND);
    }

    /** handle이 PG 취소 승인·거절·기술 실패를 각각 위임하는지 확인한다. */
    @Test
    void handleAllPgResults() {
        PgClient client = mock(PgClient.class);
        PaymentCancelResultService resultService = mock(PaymentCancelResultService.class);
        PaymentCancelProcessingService processing = mock(PaymentCancelProcessingService.class);
        PaymentCancel cancel = mock(PaymentCancel.class);
        when(cancel.getAmount()).thenReturn(100L);
        when(cancel.getIdempotencyKey()).thenReturn("K");
        when(resultService.getEntity(2L)).thenReturn(cancel);
        PgCancelResult approved = PgCancelResult.builder().status(PgResultStatus.APPROVED).build();
        PgCancelResult rejected = PgCancelResult.builder().status(PgResultStatus.REJECTED).build();
        when(client.cancel(org.mockito.ArgumentMatchers.any())).thenReturn(approved, rejected)
                .thenThrow(new PgClientException(PgErrorType.ERROR, "error"));
        PaymentCancelEventListener listener = new PaymentCancelEventListener(client, resultService, processing);
        PaymentCancelCreatedEvent event = new PaymentCancelCreatedEvent(1L, 2L, 3L);
        listener.handle(event); listener.handle(event); listener.handle(event);
        verify(processing).approve(1L, 2L, 3L, approved);
        verify(processing).reject(1L, 2L, 3L, rejected);
        verify(processing).failSystem(2L, 3L);
    }

    private static PaymentCancelCommand command() {
        return PaymentCancelCommand.builder().paymentId(1L).cancelType(CancelType.FULL)
                .amount(100).idempotencyKey("K").build();
    }
}
