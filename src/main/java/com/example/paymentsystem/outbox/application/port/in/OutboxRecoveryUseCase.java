package com.example.paymentsystem.outbox.application.port.in;

/** 고립된 아웃박스 이벤트 한 건의 재처리를 요청하는 입력 포트다. */
public interface OutboxRecoveryUseCase {

    /** 아웃박스 식별자에 해당하는 결제 또는 취소 이벤트를 재처리한다. */
    void recover(Long outboxId);
}
