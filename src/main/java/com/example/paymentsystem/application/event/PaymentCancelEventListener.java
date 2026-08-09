package com.example.paymentsystem.application.event;

import com.example.paymentsystem.application.service.cancel.PaymentCancelProcessingService;
import com.example.paymentsystem.application.service.cancel.PaymentCancelResultService;
import com.example.paymentsystem.application.port.out.pg.PgCancelCommand;
import com.example.paymentsystem.application.port.out.pg.PgCancelResult;
import com.example.paymentsystem.application.port.out.pg.PgClient;
import com.example.paymentsystem.application.port.out.pg.PgClientException;
import com.example.paymentsystem.application.port.out.pg.PgResultStatus;
import com.example.paymentsystem.domain.entity.cancel.PaymentCancel;
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
