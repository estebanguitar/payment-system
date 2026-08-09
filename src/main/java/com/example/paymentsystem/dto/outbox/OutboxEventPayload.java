package com.example.paymentsystem.dto.outbox;

/** 아웃박스 재처리에 필요한 결제와 취소 식별자만 직렬화한다. */
public record OutboxEventPayload(Long paymentId, Long cancelId) {
}
