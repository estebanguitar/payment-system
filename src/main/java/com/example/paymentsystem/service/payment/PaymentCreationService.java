package com.example.paymentsystem.service.payment;

import com.example.paymentsystem.dto.payment.PaymentCommand;

import com.example.paymentsystem.service.outbox.PaymentCreatedEvent;
import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.service.idempotency.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.dto.outbox.OutboxEventPayload;
import com.example.paymentsystem.domain.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.repository.customer.CustomerRepository;
import com.example.paymentsystem.domain.idempotency.IdempotencyRecord;
import com.example.paymentsystem.repository.idempotency.IdempotencyRecordRepository;
import com.example.paymentsystem.domain.idempotency.IdempotencyRequestType;
import com.example.paymentsystem.repository.outbox.PaymentOutboxRepository;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.repository.wallet.WalletRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PENDING 결제와 멱등·아웃박스 레코드를 원자적으로 생성한다. */
@Service
@RequiredArgsConstructor
public class PaymentCreationService {
    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final IdempotencyFingerprintGenerator fingerprintGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /** 결제 요청을 저장하고 커밋 후 처리할 이벤트를 발행한다. */
    @Transactional
    public Long createPending(PaymentCommand command) {
        if (!customerRepository.existsByCustomerId(command.getCustomerId())) {
            throw new ApplicationException(ApplicationErrorCode.CUSTOMER_NOT_FOUND);
        }
        if (walletRepository.findByCustomerId(command.getCustomerId()).isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.WALLET_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        IdempotencyRecord record = IdempotencyRecord.reserve(command.getIdempotencyKey(),
                IdempotencyRequestType.PAYMENT,
                fingerprintGenerator.payment(command.getCustomerId(), command.getAmount()), now);
        idempotencyRepository.saveAndFlush(record);
        Payment payment = paymentRepository.save(Payment.createPending(command.getIdempotencyKey(),
                command.getCustomerId(), command.getAmount(), now));
        paymentRepository.flush();
        PaymentOutbox outbox = outboxRepository.save(PaymentOutbox.createPaymentRequested(
                payment.getId(), command.getIdempotencyKey(), payload(payment.getId(), null), now));
        outboxRepository.flush();
        record.linkResource(payment.getId(), now);
        eventPublisher.publishEvent(new PaymentCreatedEvent(payment.getId(), outbox.getId()));
        return payment.getId();
    }

    private String payload(Long paymentId, Long cancelId) {
        try {
            return objectMapper.writeValueAsString(new OutboxEventPayload(paymentId, cancelId));
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR, null, exception);
        }
    }
}
