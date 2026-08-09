package com.example.paymentsystem.service.outbox;

import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.domain.outbox.OutboxStatus;
import com.example.paymentsystem.domain.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.domain.payment.PaymentCancel;
import com.example.paymentsystem.dto.outbox.OutboxEventPayload;
import com.example.paymentsystem.integration.pg.pg.PgApprovalCommand;
import com.example.paymentsystem.integration.pg.pg.PgApprovalResult;
import com.example.paymentsystem.integration.pg.pg.PgCancelCommand;
import com.example.paymentsystem.integration.pg.pg.PgCancelResult;
import com.example.paymentsystem.integration.pg.pg.PgClient;
import com.example.paymentsystem.integration.pg.pg.PgClientException;
import com.example.paymentsystem.integration.pg.pg.PgResultStatus;
import com.example.paymentsystem.repository.outbox.PaymentOutboxRepository;
import com.example.paymentsystem.repository.payment.PaymentCancelRepository;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.service.payment.PaymentCancelProcessingService;
import com.example.paymentsystem.service.payment.PaymentProcessingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 즉시 이벤트와 Scheduler가 공유하는 Outbox PG 후속 처리를 수행한다. */
@Service
@RequiredArgsConstructor
public class OutboxProcessor {
    private final PaymentOutboxRepository outboxRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository cancelRepository;
    private final PgClient pgClient;
    private final PaymentProcessingService paymentProcessingService;
    private final PaymentCancelProcessingService cancelProcessingService;
    private final OutboxStatusService statusService;
    private final ObjectMapper objectMapper;

    /** PENDING/RETRY 이벤트 한 건을 검증하고 유형에 맞는 PG 처리를 수행한다. */
    public void process(Long outboxId) {
        PaymentOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR));
        if (outbox.getStatus() != OutboxStatus.PENDING && outbox.getStatus() != OutboxStatus.RETRY) {
            return;
        }
        try {
            OutboxEventPayload payload = payload(outbox.getPayload());
            if (!outbox.getPaymentId().equals(payload.paymentId())) {
                throw new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR);
            }
            switch (outbox.getEventType()) {
                case PAYMENT_REQUESTED -> processPayment(outboxId, payment(payload.paymentId()));
                case PAYMENT_CANCEL_REQUESTED -> processCancel(outboxId, payload, cancel(payload.cancelId()));
            }
        } catch (RuntimeException exception) {
            statusService.recordFailure(outboxId);
            throw exception;
        }
    }

    private void processPayment(Long outboxId, Payment payment) {
        try {
            PgApprovalResult result = pgClient.approve(PgApprovalCommand.builder()
                    .paymentId(payment.getId()).customerId(payment.getCustomerId()).amount(payment.getAmount())
                    .idempotencyKey(payment.getIdempotencyKey()).build());
            if (result.getStatus() == PgResultStatus.APPROVED) {
                paymentProcessingService.approve(payment.getId(), outboxId, result);
            } else {
                paymentProcessingService.reject(payment.getId(), outboxId, result);
            }
        } catch (PgClientException exception) {
            paymentProcessingService.failSystem(payment.getId(), outboxId);
        }
    }

    private void processCancel(Long outboxId, OutboxEventPayload payload, PaymentCancel cancel) {
        try {
            PgCancelResult result = pgClient.cancel(PgCancelCommand.builder()
                    .paymentId(payload.paymentId()).cancelId(cancel.getId()).amount(cancel.getAmount())
                    .idempotencyKey(cancel.getIdempotencyKey()).build());
            if (result.getStatus() == PgResultStatus.APPROVED) {
                cancelProcessingService.approve(payload.paymentId(), cancel.getId(), outboxId, result);
            } else {
                cancelProcessingService.reject(payload.paymentId(), cancel.getId(), outboxId, result);
            }
        } catch (PgClientException exception) {
            cancelProcessingService.failSystem(cancel.getId(), outboxId);
        }
    }

    private Payment payment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND));
    }

    private PaymentCancel cancel(Long id) {
        return cancelRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.CANCEL_NOT_FOUND));
    }

    private OutboxEventPayload payload(String value) {
        try {
            return objectMapper.readValue(value, OutboxEventPayload.class);
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR, null, exception);
        }
    }
}
