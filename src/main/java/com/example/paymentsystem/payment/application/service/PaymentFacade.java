package com.example.paymentsystem.payment.application.service;

import com.example.paymentsystem.payment.application.dto.PaymentCommand;
import com.example.paymentsystem.payment.application.dto.PaymentResult;

import com.example.paymentsystem.idempotency.application.service.IdempotencyConflictResolver;
import com.example.paymentsystem.idempotency.application.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.idempotency.domain.IdempotencyRequestType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** 결제 최초 요청과 멱등 재요청을 하나의 진입점으로 조율한다. */
@Service
@RequiredArgsConstructor
public class PaymentFacade {
    private final PaymentCreationService creationService;
    private final PaymentResultService resultService;
    private final IdempotencyConflictResolver conflictResolver;
    private final IdempotencyFingerprintGenerator fingerprintGenerator;

    /** 결제를 생성하거나 동일한 기존 요청 결과를 반환한다. */
    public PaymentResult requestPayment(PaymentCommand command) {
        Long paymentId;
        try {
            paymentId = creationService.createPending(command);
        } catch (DataIntegrityViolationException exception) {
            paymentId = conflictResolver.resolve(command.getIdempotencyKey(), IdempotencyRequestType.PAYMENT,
                    fingerprintGenerator.payment(command.getCustomerId(), command.getAmount()));
        }
        return resultService.get(paymentId);
    }
}
