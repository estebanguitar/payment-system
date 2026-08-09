package com.example.paymentsystem.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.dto.payment.PaymentCommand;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.service.idempotency.IdempotencyConflictResolver;
import com.example.paymentsystem.service.idempotency.IdempotencyFingerprintGenerator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/** 결제 Facade의 최초 요청과 멱등 재요청 결과 변환을 검증한다. */
class PaymentServiceTest {
    /** 최초 생성 ID로 결제를 조회해 DTO로 반환하는지 확인한다. */
    @Test
    void requestPayment() {
        PaymentCreationService creation = mock(PaymentCreationService.class);
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentCommand command = command();
        Payment payment = payment(1L);
        when(creation.createPending(command)).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentFacade facade = new PaymentFacade(creation, repository,
                mock(IdempotencyConflictResolver.class), new IdempotencyFingerprintGenerator());

        assertThat(facade.requestPayment(command).getPaymentId()).isEqualTo(1L);
    }

    /** 유일성 충돌 시 멱등 리소스 ID를 조회해 기존 결과를 반환하는지 확인한다. */
    @Test
    void requestPaymentDuplicate() {
        PaymentCreationService creation = mock(PaymentCreationService.class);
        PaymentRepository repository = mock(PaymentRepository.class);
        IdempotencyConflictResolver resolver = mock(IdempotencyConflictResolver.class);
        PaymentCommand command = command();
        when(creation.createPending(command)).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(resolver.resolve(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(payment(2L)));
        PaymentFacade facade = new PaymentFacade(creation, repository, resolver,
                new IdempotencyFingerprintGenerator());

        assertThat(facade.requestPayment(command).getPaymentId()).isEqualTo(2L);
    }

    private static Payment payment(Long id) {
        Payment payment = Payment.createPending("K", "C", 100, LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }

    private static PaymentCommand command() {
        return PaymentCommand.builder().customerId("C").amount(100).idempotencyKey("K").build();
    }
}
