package com.example.paymentsystem.application.event;

/** 커밋된 결제와 아웃박스를 후속 PG 처리에 전달한다. */
public record PaymentCreatedEvent(Long paymentId, Long outboxId) {
}
