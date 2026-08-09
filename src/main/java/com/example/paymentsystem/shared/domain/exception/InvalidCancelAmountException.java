package com.example.paymentsystem.shared.domain.exception;

/** 취소 유형 또는 잔여 취소 가능 금액에 맞지 않는 요청을 나타낸다. */
public class InvalidCancelAmountException extends DomainException {

    /** 표준 취소 금액 오류 코드와 메시지로 예외를 생성한다. */
    public InvalidCancelAmountException() {
        super(DomainErrorCode.INVALID_CANCEL_AMOUNT);
    }
}
