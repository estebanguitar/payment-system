package com.example.paymentsystem.architecture.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.dto.common.PageResult;
import com.example.paymentsystem.dto.payment.query.PaymentCancelView;
import com.example.paymentsystem.dto.payment.query.WalletTransactionView;
import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.integration.pg.PgClientException;
import com.example.paymentsystem.integration.pg.PgErrorType;
import com.example.paymentsystem.service.customer.CustomerQueryService;
import com.example.paymentsystem.domain.payment.PaymentCancel;
import com.example.paymentsystem.domain.customer.Customer;
import com.example.paymentsystem.domain.wallet.WalletTransaction;
import com.example.paymentsystem.repository.customer.CustomerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** 공통 예외·조회 변환 DTO·고객 조회 메서드의 독립 동작을 검증한다. */
class ApplicationCommonUnitTest {
    /** ApplicationException의 모든 생성자가 코드·리소스·원인을 보존하는지 확인한다. */
    @Test
    void createApplicationExceptions() {
        RuntimeException cause = new RuntimeException("cause");
        assertThat(new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR).getCode())
                .isEqualTo("SYSTEM_ERROR");
        assertThat(new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND, 1L).getResourceId())
                .isEqualTo(1L);
        assertThat(new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR, 1L, cause).getCause())
                .isSameAs(cause);
    }

    /** PgClientException의 두 생성자가 오류 유형과 원인을 보존하는지 확인한다. */
    @Test
    void createPgClientExceptions() {
        RuntimeException cause = new RuntimeException("cause");
        assertThat(new PgClientException(PgErrorType.ERROR, "error").getErrorType())
                .isEqualTo(PgErrorType.ERROR);
        assertThat(new PgClientException(PgErrorType.TIMEOUT, "timeout", cause).getCause()).isSameAs(cause);
    }

    /** PageResult.from이 Spring Page를 1-based 페이지 DTO로 변환하는지 확인한다. */
    @Test
    void convertPageResult() {
        PageResult<String> result = PageResult.from(new PageImpl<>(List.of("A"), PageRequest.of(1, 1), 3));
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    /** 조회용 변환 메서드가 엔티티 필드를 DTO에 복사하는지 확인한다. */
    @Test
    void convertQueryViews() {
        PaymentCancel cancel = mock(PaymentCancel.class);
        when(cancel.getId()).thenReturn(1L);
        WalletTransaction transaction = mock(WalletTransaction.class);
        when(transaction.getId()).thenReturn(2L);
        assertThat(PaymentCancelView.from(cancel).cancelId()).isEqualTo(1L);
        assertThat(WalletTransactionView.from(transaction).transactionId()).isEqualTo(2L);
    }

    /** getCustomer가 고객을 반환하고 미존재 고객을 표준 오류로 변환하는지 확인한다. */
    @Test
    void getCustomer() {
        CustomerRepository repository = mock(CustomerRepository.class);
        Customer customer = mock(Customer.class);
        when(repository.findByCustomerId("C")).thenReturn(Optional.of(customer));
        CustomerQueryService service = new CustomerQueryService(repository);
        assertThat(service.getCustomer("C")).isSameAs(customer);
        assertThatThrownBy(() -> service.getCustomer("NONE")).extracting("errorCode")
                .isEqualTo(ApplicationErrorCode.CUSTOMER_NOT_FOUND);
    }
}
