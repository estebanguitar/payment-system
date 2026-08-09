package com.example.paymentsystem.application.service.outbox;

import com.example.paymentsystem.application.dto.outbox.OutboxEventPayload;
import com.example.paymentsystem.application.event.PaymentCancelCreatedEvent;
import com.example.paymentsystem.application.event.PaymentCancelEventListener;
import com.example.paymentsystem.application.event.PaymentCreatedEvent;
import com.example.paymentsystem.application.event.PaymentEventListener;
import com.example.paymentsystem.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.application.exception.ApplicationException;
import com.example.paymentsystem.application.port.in.outbox.OutboxRecoveryUseCase;
import com.example.paymentsystem.domain.entity.outbox.OutboxStatus;
import com.example.paymentsystem.domain.entity.outbox.PaymentOutbox;
import com.example.paymentsystem.infrastructure.repository.outbox.PaymentOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 고립된 결제·취소 아웃박스를 유형별 PG 처리 흐름으로 다시 전달한다. */
@Service
@RequiredArgsConstructor
public class OutboxRecoveryService implements OutboxRecoveryUseCase {
    private final PaymentOutboxRepository outboxRepository;
    private final PaymentEventListener paymentListener;
    private final PaymentCancelEventListener cancelListener;
    private final OutboxStatusService statusService;
    private final ObjectMapper objectMapper;

    /** INIT 이벤트만 재처리하며 내부 반영 실패는 별도 트랜잭션으로 횟수를 기록한다. */
    @Override
    public void recover(Long outboxId) {
        PaymentOutbox outbox = get(outboxId);
        if (outbox.getStatus() != OutboxStatus.INIT) {
            return;
        }
        OutboxEventPayload payload = payload(outbox.getPayload());
        if (!outbox.getPaymentId().equals(payload.paymentId())) {
            statusService.recordFailure(outboxId);
            throw new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR);
        }
        try {
            switch (outbox.getEventType()) {
                case PAYMENT_REQUESTED -> paymentListener.handle(
                        new PaymentCreatedEvent(payload.paymentId(), outboxId));
                case PAYMENT_CANCEL_REQUESTED -> cancelListener.handle(
                        new PaymentCancelCreatedEvent(payload.paymentId(), payload.cancelId(), outboxId));
            }
        } catch (RuntimeException exception) {
            statusService.recordFailure(outboxId);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    protected PaymentOutbox get(Long outboxId) {
        return outboxRepository.findById(outboxId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR));
    }

    private OutboxEventPayload payload(String value) {
        try {
            return objectMapper.readValue(value, OutboxEventPayload.class);
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR, null, exception);
        }
    }
}
