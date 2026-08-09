package com.example.paymentsystem.shared.domain.exception;

/** 0원·음수 또는 자료형 범위를 벗어난 금액 요청을 나타낸다. */
public class InvalidAmountException extends DomainException {

    /** 표준 금액 오류 코드로 예외를 생성한다. */
    public InvalidAmountException() {
        super(DomainErrorCode.INVALID_AMOUNT);
    }

    /** overflow처럼 세분화된 금액 오류 코드로 예외를 생성한다. */
    public InvalidAmountException(DomainErrorCode errorCode) {
        super(errorCode);
    }
}
