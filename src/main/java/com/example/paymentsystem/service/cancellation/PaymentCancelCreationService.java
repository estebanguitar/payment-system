package com.example.paymentsystem.service.cancellation;

import com.example.paymentsystem.dto.cancellation.PaymentCancelCommand;

import com.example.paymentsystem.service.outbox.PaymentCancelCreatedEvent;
import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.service.idempotency.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.dto.outbox.OutboxEventPayload;
import com.example.paymentsystem.domain.cancellation.PaymentCancel;
import com.example.paymentsystem.domain.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.repository.cancellation.PaymentCancelRepository;
import com.example.paymentsystem.domain.idempotency.IdempotencyRecord;
import com.example.paymentsystem.repository.idempotency.IdempotencyRecordRepository;
import com.example.paymentsystem.domain.idempotency.IdempotencyRequestType;
import com.example.paymentsystem.repository.outbox.PaymentOutboxRepository;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PENDING 취소와 멱등·아웃박스 레코드를 원자적으로 생성한다. */
@Service
@RequiredArgsConstructor
public class PaymentCancelCreationService {
    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository cancelRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final IdempotencyFingerprintGenerator fingerprintGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /** 잠긴 원 결제를 검증하고 취소 요청을 저장한 후 커밋 이벤트를 발행한다. */
    @Transactional
    public Long createPending(PaymentCancelCommand command) {
        LocalDateTime now = LocalDateTime.now();
        IdempotencyRecord record = IdempotencyRecord.reserve(command.getIdempotencyKey(),
                IdempotencyRequestType.PAYMENT_CANCEL,
                fingerprintGenerator.cancel(command.getPaymentId(), command.getCancelType(), command.getAmount()), now);
        idempotencyRepository.saveAndFlush(record);
        Payment payment = paymentRepository.findByIdWithLock(command.getPaymentId())
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND));
        PaymentCancel cancel = cancelRepository.save(PaymentCancel.createPending(payment.getId(),
                command.getIdempotencyKey(), command.getCancelType(), command.getAmount(),
                payment.remainingCancelableAmount(), now));
        cancelRepository.flush();
        PaymentOutbox outbox = outboxRepository.save(PaymentOutbox.createPaymentCancelRequested(
                payment.getId(), command.getIdempotencyKey(), payload(payment.getId(), cancel.getId()), now));
        outboxRepository.flush();
        record.linkResource(cancel.getId(), now);
        eventPublisher.publishEvent(new PaymentCancelCreatedEvent(payment.getId(), cancel.getId(), outbox.getId()));
        return cancel.getId();
    }

    private String payload(Long paymentId, Long cancelId) {
        try {
            return objectMapper.writeValueAsString(new OutboxEventPayload(paymentId, cancelId));
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR, null, exception);
        }
    }
}
