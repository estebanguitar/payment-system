package com.example.paymentsystem.infrastructure.outbox;

/** 스케줄러가 고립된 아웃박스 한 건의 Application 재처리를 요청하는 경계다. */
public interface OutboxRecoveryPort {

    /** 아웃박스 식별자에 해당하는 결제 이벤트를 독립적으로 재처리한다. */
    void recover(Long outboxId);
}
