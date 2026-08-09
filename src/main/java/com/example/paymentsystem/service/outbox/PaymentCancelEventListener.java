package com.example.paymentsystem.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 취소 커밋 이벤트를 OutboxProcessor에 전달하는 즉시 실행 Adapter다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelEventListener {
    private final OutboxProcessor processor;

    /** 취소 커밋 직후 Outbox 처리를 트리거하고 실패는 Scheduler 복구 대상으로 남긴다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentCancelCreatedEvent event) {
        try {
            processor.process(event.outboxId());
        } catch (RuntimeException exception) {
            log.warn("취소 Outbox 즉시 처리에 실패했습니다. outboxId={}", event.outboxId(), exception);
        }
    }
}
