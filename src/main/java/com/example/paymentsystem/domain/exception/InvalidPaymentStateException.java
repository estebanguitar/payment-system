package com.example.paymentsystem.domain.exception;

/** 현재 결제·취소·아웃박스 상태에서 허용되지 않은 전이를 나타낸다. */
public class InvalidPaymentStateException extends DomainException {

    /** 표준 상태 전이 오류 코드와 메시지로 예외를 생성한다. */
    public InvalidPaymentStateException() {
        super(DomainErrorCode.INVALID_PAYMENT_STATE);
    }
}
