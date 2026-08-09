package com.example.paymentsystem.shared.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.shared.domain.exception.DomainErrorCode;
import com.example.paymentsystem.shared.domain.exception.DomainException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 계층별 오류 코드가 API 계약의 HTTP 상태로 변환되는지 검증한다. */
class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** 리소스 미존재 Application 오류가 모두 404로 변환되는지 검증한다. */
    @ParameterizedTest
    @EnumSource(value = ApplicationErrorCode.class,
            names = {"CUSTOMER_NOT_FOUND", "WALLET_NOT_FOUND", "PAYMENT_NOT_FOUND", "CANCEL_NOT_FOUND"})
    void applicationNotFoundErrorsReturn404(ApplicationErrorCode code) {
        assertStatus(handler.handleApplication(new ApplicationException(code)), HttpStatus.NOT_FOUND, code.getCode());
    }

    /** 충돌과 시스템 Application 오류가 각각 409와 500으로 변환되는지 검증한다. */
    @ParameterizedTest
    @MethodSource("applicationStatusCases")
    void applicationErrorsReturnContractStatus(ApplicationErrorCode code, HttpStatus status) {
        assertStatus(handler.handleApplication(new ApplicationException(code)), status, code.getCode());
    }

    /** 주요 Domain 상태 오류가 계약된 409와 500으로 변환되는지 검증한다. */
    @ParameterizedTest
    @MethodSource("domainStatusCases")
    void domainErrorsReturnContractStatus(DomainErrorCode code, HttpStatus status) {
        assertStatus(handler.handleDomain(new DomainException(code)), status, code.getCode());
    }

    private static Object[][] applicationStatusCases() {
        return new Object[][] {
                {ApplicationErrorCode.IDEMPOTENCY_CONFLICT, HttpStatus.CONFLICT},
                {ApplicationErrorCode.SYSTEM_ERROR, HttpStatus.INTERNAL_SERVER_ERROR}
        };
    }

    private static Object[][] domainStatusCases() {
        return new Object[][] {
                {DomainErrorCode.INVALID_PAYMENT_STATE, HttpStatus.CONFLICT},
                {DomainErrorCode.OUTBOX_RETRY_OVERFLOW, HttpStatus.INTERNAL_SERVER_ERROR}
        };
    }

    private static void assertStatus(ResponseEntity<? extends ApiResponse<?>> response, HttpStatus status,
            String errorCode) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(errorCode);
    }
}
