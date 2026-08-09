package com.example.paymentsystem.service.cancellation;

import com.example.paymentsystem.dto.cancellation.PaymentCancelCommand;
import com.example.paymentsystem.dto.cancellation.PaymentCancelResult;

import com.example.paymentsystem.service.idempotency.IdempotencyConflictResolver;
import com.example.paymentsystem.service.idempotency.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.domain.idempotency.IdempotencyRequestType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** 취소 최초 요청과 멱등 재요청을 하나의 진입점으로 조율한다. */
@Service
@RequiredArgsConstructor
public class PaymentCancelFacade {
    private final PaymentCancelCreationService creationService;
    private final PaymentCancelResultService resultService;
    private final IdempotencyConflictResolver conflictResolver;
    private final IdempotencyFingerprintGenerator fingerprintGenerator;

    /** 취소를 생성하거나 동일한 기존 요청 결과를 반환한다. */
    public PaymentCancelResult cancel(PaymentCancelCommand command) {
        Long cancelId;
        try {
            cancelId = creationService.createPending(command);
        } catch (DataIntegrityViolationException exception) {
            cancelId = conflictResolver.resolve(command.getIdempotencyKey(), IdempotencyRequestType.PAYMENT_CANCEL,
                    fingerprintGenerator.cancel(command.getPaymentId(), command.getCancelType(), command.getAmount()));
        }
        return resultService.get(cancelId);
    }
}
