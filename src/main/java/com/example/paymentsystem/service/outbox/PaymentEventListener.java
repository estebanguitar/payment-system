package com.example.paymentsystem.service.outbox;

import com.example.paymentsystem.service.payment.PaymentProcessingService;
import com.example.paymentsystem.dto.payment.PaymentResult;
import com.example.paymentsystem.service.payment.PaymentResultService;
import com.example.paymentsystem.integration.pg.pg.PgApprovalCommand;
import com.example.paymentsystem.integration.pg.pg.PgApprovalResult;
import com.example.paymentsystem.integration.pg.pg.PgClient;
import com.example.paymentsystem.integration.pg.pg.PgClientException;
import com.example.paymentsystem.integration.pg.pg.PgResultStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 결제 저장 커밋 후 트랜잭션 없이 PG를 호출하고 결과 반영을 위임한다. */
@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private final PgClient pgClient;
    private final PaymentResultService resultService;
    private final PaymentProcessingService processingService;

    /** 결제 승인 결과를 분류해 별도 결과 반영 트랜잭션으로 전달한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentCreatedEvent event) {
        PaymentResult payment = resultService.get(event.paymentId());
        try {
            PgApprovalResult result = pgClient.approve(PgApprovalCommand.builder()
                    .paymentId(payment.getPaymentId()).customerId(payment.getCustomerId())
                    .amount(payment.getAmount()).idempotencyKey(payment.getIdempotencyKey()).build());
            if (result.getStatus() == PgResultStatus.APPROVED) {
                processingService.approve(event.paymentId(), event.outboxId(), result);
            } else {
                processingService.reject(event.paymentId(), event.outboxId(), result);
            }
        } catch (PgClientException exception) {
            processingService.failSystem(event.paymentId(), event.outboxId());
        }
    }
}
