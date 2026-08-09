package com.example.paymentsystem.shared.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 도메인 예외가 enum의 표준 코드와 메시지를 재사용하는지 검증한다. */
class DomainErrorCodeTest {

    /** 잔액 부족 예외가 공통 오류 enum의 코드와 메시지를 그대로 제공하는지 확인한다. */
    @Test
    void reuseCodeAndMessageFromErrorCode() {
        DomainException exception = new InsufficientBalanceException();

        assertThat(exception.getErrorCode()).isEqualTo(DomainErrorCode.INSUFFICIENT_BALANCE);
        assertThat(exception.getCode()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(exception.getMessage()).isEqualTo("지갑 잔액이 부족합니다.");
    }
}
