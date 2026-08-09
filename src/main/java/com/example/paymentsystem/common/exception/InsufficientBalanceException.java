package com.example.paymentsystem.common.exception;

/** 요청 금액보다 지갑 잔액이 적어 차감할 수 없음을 나타낸다. */
public class InsufficientBalanceException extends DomainException {

    /** 표준 잔액 부족 오류 코드와 메시지로 예외를 생성한다. */
    public InsufficientBalanceException() {
        super(DomainErrorCode.INSUFFICIENT_BALANCE);
    }
}
