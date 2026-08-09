package com.example.paymentsystem.service.outbox;

import com.example.paymentsystem.service.cancellation.PaymentCancelProcessingService;
import com.example.paymentsystem.service.cancellation.PaymentCancelResultService;
import com.example.paymentsystem.integration.pg.pg.PgCancelCommand;
import com.example.paymentsystem.integration.pg.pg.PgCancelResult;
import com.example.paymentsystem.integration.pg.pg.PgClient;
import com.example.paymentsystem.integration.pg.pg.PgClientException;
import com.example.paymentsystem.integration.pg.pg.PgResultStatus;
import com.example.paymentsystem.domain.cancellation.PaymentCancel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 취소 저장 커밋 후 트랜잭션 없이 PG를 호출하고 결과 반영을 위임한다. */
@Component
@RequiredArgsConstructor
public class PaymentCancelEventListener {
    private final PgClient pgClient;
    private final PaymentCancelResultService resultService;
    private final PaymentCancelProcessingService processingService;

    /** PG 취소 결과를 분류해 별도 결과 반영 트랜잭션으로 전달한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentCancelCreatedEvent event) {
        PaymentCancel cancel = resultService.getEntity(event.cancelId());
        try {
            PgCancelResult result = pgClient.cancel(PgCancelCommand.builder()
                    .paymentId(event.paymentId()).cancelId(event.cancelId()).amount(cancel.getAmount())
                    .idempotencyKey(cancel.getIdempotencyKey()).build());
            if (result.getStatus() == PgResultStatus.APPROVED) {
                processingService.approve(event.paymentId(), event.cancelId(), event.outboxId(), result);
            } else {
                processingService.reject(event.paymentId(), event.cancelId(), event.outboxId(), result);
            }
        } catch (PgClientException exception) {
            processingService.failSystem(event.cancelId(), event.outboxId());
        }
    }
}
