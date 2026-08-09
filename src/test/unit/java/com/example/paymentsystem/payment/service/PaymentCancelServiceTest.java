package com.example.paymentsystem.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.domain.payment.CancelType;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.domain.payment.PaymentCancel;
import com.example.paymentsystem.dto.payment.PaymentCancelCommand;
import com.example.paymentsystem.repository.payment.PaymentCancelRepository;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.service.idempotency.IdempotencyConflictResolver;
import com.example.paymentsystem.service.idempotency.IdempotencyFingerprintGenerator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/** 결제 취소 Facade의 최초 요청과 멱등 재요청 결과 조립을 검증한다. */
class PaymentCancelServiceTest {
    /** 생성·중복 경로 모두 취소와 원 결제를 조회해 결과를 반환하는지 확인한다. */
    @Test
    void cancelAndResolveDuplicate() {
        PaymentCancelCreationService creation = mock(PaymentCancelCreationService.class);
        PaymentCancelRepository cancelRepository = mock(PaymentCancelRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        IdempotencyConflictResolver resolver = mock(IdempotencyConflictResolver.class);
        PaymentCancelCommand command = command();
        Payment payment = payment();
        when(creation.createPending(command)).thenReturn(1L);
        when(cancelRepository.findById(1L)).thenReturn(Optional.of(cancel(1L)));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentCancelFacade facade = new PaymentCancelFacade(creation, cancelRepository, paymentRepository, resolver,
                new IdempotencyFingerprintGenerator());
        assertThat(facade.cancel(command).getCancelId()).isEqualTo(1L);

        when(creation.createPending(command)).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(resolver.resolve(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(2L);
        when(cancelRepository.findById(2L)).thenReturn(Optional.of(cancel(2L)));
        assertThat(facade.cancel(command).getCancelId()).isEqualTo(2L);
    }

    private static Payment payment() {
        Payment payment = Payment.createPending("PK", "C", 100, LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", 1L);
        return payment;
    }

    private static PaymentCancel cancel(Long id) {
        PaymentCancel cancel = PaymentCancel.createPending(1L, "CK", CancelType.FULL, 100, 100,
                LocalDateTime.now());
        ReflectionTestUtils.setField(cancel, "id", id);
        return cancel;
    }

    private static PaymentCancelCommand command() {
        return PaymentCancelCommand.builder().paymentId(1L).cancelType(CancelType.FULL)
                .amount(100).idempotencyKey("K").build();
    }
}
