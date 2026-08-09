package com.example.paymentsystem.application.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.application.dto.payment.PaymentCommand;
import com.example.paymentsystem.application.dto.payment.PaymentResult;
import com.example.paymentsystem.application.event.PaymentCreatedEvent;
import com.example.paymentsystem.application.event.PaymentEventListener;
import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.pg.application.port.out.pg.PgApprovalResult;
import com.example.paymentsystem.pg.application.port.out.pg.PgClient;
import com.example.paymentsystem.pg.application.port.out.pg.PgClientException;
import com.example.paymentsystem.pg.application.port.out.pg.PgErrorType;
import com.example.paymentsystem.pg.application.port.out.pg.PgResultStatus;
import com.example.paymentsystem.idempotency.application.service.IdempotencyConflictResolver;
import com.example.paymentsystem.idempotency.application.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.domain.entity.payment.Payment;
import com.example.paymentsystem.infrastructure.repository.payment.PaymentRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/** 결제 Facade·결과 조회·이벤트 리스너의 공개 메서드별 분기를 검증한다. */
class PaymentServiceTest {
    /** requestPayment가 최초 생성 결과를 조회하는지 확인한다. */
    @Test
    void requestPayment() {
        PaymentCreationService creation = mock(PaymentCreationService.class);
        PaymentResultService resultService = mock(PaymentResultService.class);
        PaymentCommand command = command();
        PaymentResult expected = PaymentResult.builder().paymentId(1L).build();
        when(creation.createPending(command)).thenReturn(1L);
        when(resultService.get(1L)).thenReturn(expected);
        PaymentFacade facade = new PaymentFacade(creation, resultService,
                mock(IdempotencyConflictResolver.class), new IdempotencyFingerprintGenerator());
        assertThat(facade.requestPayment(command)).isSameAs(expected);
    }

    /** requestPayment가 유일성 충돌 시 동일 멱등 리소스를 재조회하는지 확인한다. */
    @Test
    void requestPaymentDuplicate() {
        PaymentCreationService creation = mock(PaymentCreationService.class);
        PaymentResultService resultService = mock(PaymentResultService.class);
        IdempotencyConflictResolver resolver = mock(IdempotencyConflictResolver.class);
        PaymentCommand command = command();
        when(creation.createPending(command)).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(resolver.resolve(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(2L);
        when(resultService.get(2L)).thenReturn(PaymentResult.builder().paymentId(2L).build());
        PaymentFacade facade = new PaymentFacade(creation, resultService, resolver,
                new IdempotencyFingerprintGenerator());
        assertThat(facade.requestPayment(command).getPaymentId()).isEqualTo(2L);
    }

    /** get이 결제를 DTO로 변환하고 미존재 결제를 표준 오류로 처리하는지 확인한다. */
    @Test
    void getPaymentResult() {
        PaymentRepository repository = mock(PaymentRepository.class);
        Payment payment = Payment.createPending("K", "C", 100, LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentResultService service = new PaymentResultService(repository);
        assertThat(service.get(1L).getPaymentId()).isEqualTo(1L);
        assertThatThrownBy(() -> service.get(2L)).extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.PAYMENT_NOT_FOUND);
    }

    /** handle이 PG 승인·거절·기술 실패를 알맞은 처리 메서드로 전달하는지 확인한다. */
    @Test
    void handleAllPgResults() {
        PgClient client = mock(PgClient.class);
        PaymentResultService resultService = mock(PaymentResultService.class);
        PaymentProcessingService processing = mock(PaymentProcessingService.class);
        PaymentEventListener listener = new PaymentEventListener(client, resultService, processing);
        PaymentResult payment = PaymentResult.builder().paymentId(1L).customerId("C")
                .amount(100).idempotencyKey("K").build();
        when(resultService.get(1L)).thenReturn(payment);
        PgApprovalResult approved = PgApprovalResult.builder().status(PgResultStatus.APPROVED).build();
        PgApprovalResult rejected = PgApprovalResult.builder().status(PgResultStatus.REJECTED).build();
        when(client.approve(org.mockito.ArgumentMatchers.any())).thenReturn(approved, rejected)
                .thenThrow(new PgClientException(PgErrorType.TIMEOUT, "timeout"));
        PaymentCreatedEvent event = new PaymentCreatedEvent(1L, 2L);
        listener.handle(event);
        listener.handle(event);
        listener.handle(event);
        verify(processing).approve(1L, 2L, approved);
        verify(processing).reject(1L, 2L, rejected);
        verify(processing).failSystem(1L, 2L);
    }

    private static PaymentCommand command() {
        return PaymentCommand.builder().customerId("C").amount(100).idempotencyKey("K").build();
    }
}
