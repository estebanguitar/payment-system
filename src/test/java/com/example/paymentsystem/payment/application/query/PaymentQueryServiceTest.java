package com.example.paymentsystem.payment.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.payment.application.dto.query.PaymentSearchCondition;
import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.payment.domain.Payment;
import com.example.paymentsystem.cancellation.infrastructure.repository.PaymentCancelRepository;
import com.example.paymentsystem.payment.infrastructure.repository.PaymentRepository;
import com.example.paymentsystem.pg.infrastructure.repository.PgResponseLogRepository;
import com.example.paymentsystem.wallet.infrastructure.repository.WalletTransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

/** 고객·운영자 결제 조회의 공개 메서드와 입력 경계값을 검증한다. */
class PaymentQueryServiceTest {
    private PaymentRepository paymentRepository;
    private PaymentQueryService service;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        service = new PaymentQueryService(paymentRepository, mock(PaymentCancelRepository.class),
                mock(WalletTransactionRepository.class), mock(PgResponseLogRepository.class));
        payment = Payment.createPending("K", "C", 100, LocalDateTime.now());
        ReflectionTestUtils.setField(payment, "id", 1L);
    }

    /** getCustomerPayment가 소유권 일치 결제를 반환하고 불일치를 숨기는지 확인한다. */
    @Test
    void getCustomerPayment() {
        when(paymentRepository.findByIdAndCustomerId(1L, "C")).thenReturn(Optional.of(payment));
        assertThat(service.getCustomerPayment("C", 1L).getPayment().getPaymentId()).isEqualTo(1L);
        assertThatThrownBy(() -> service.getCustomerPayment("OTHER", 1L))
                .extracting("errorCode").isEqualTo(ApplicationErrorCode.PAYMENT_NOT_FOUND);
    }

    /** getCustomerPayments가 1-based 페이지를 변환하고 범위를 검증하는지 확인한다. */
    @Test
    void getCustomerPayments() {
        when(paymentRepository.findAllByCustomerId(org.mockito.ArgumentMatchers.eq("C"),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));
        assertThat(service.getCustomerPayments("C", 1, 20).getContent()).hasSize(1);
        assertThatThrownBy(() -> service.getCustomerPayments("C", 0, 20))
                .extracting("errorCode").isEqualTo(ApplicationErrorCode.INVALID_PAGE_REQUEST);
    }

    /** search가 복합 조건 페이지를 반환하고 역전 날짜를 거부하는지 확인한다. */
    @Test
    @SuppressWarnings("unchecked")
    void search() {
        when(paymentRepository.findAll(org.mockito.ArgumentMatchers.any(Specification.class),
                org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(new PageImpl<>(List.of(payment)));
        PaymentSearchCondition valid = PaymentSearchCondition.builder().page(1).size(20).build();
        assertThat(service.search(valid).getTotalElements()).isEqualTo(1);
        PaymentSearchCondition invalid = PaymentSearchCondition.builder().page(1).size(20)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().minusDays(1)).build();
        assertThatThrownBy(() -> service.search(invalid)).extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.INVALID_DATE_RANGE);
    }

    /** getOperationsPayment가 상세 메타데이터를 조합하고 미존재 결제를 처리하는지 확인한다. */
    @Test
    void getOperationsPayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        assertThat(service.getOperationsPayment(1L).getPayment().getPaymentId()).isEqualTo(1L);
        assertThatThrownBy(() -> service.getOperationsPayment(2L))
                .extracting("errorCode").isEqualTo(ApplicationErrorCode.PAYMENT_NOT_FOUND);
    }
}
