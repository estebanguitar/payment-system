package com.example.paymentsystem.cancellation.application.event;

/** 커밋된 취소와 아웃박스를 후속 PG 처리에 전달한다. */
public record PaymentCancelCreatedEvent(Long paymentId, Long cancelId, Long outboxId) {
}
