package com.example.paymentsystem.application.dto.payment;

import com.example.paymentsystem.domain.entity.payment.Payment;
import com.example.paymentsystem.domain.entity.payment.PaymentFailureReason;
import com.example.paymentsystem.domain.entity.payment.PaymentStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 결제 처리 후 외부 계층에 전달할 최종 원장 결과다. */
@Getter
@Builder
public class PaymentResult {
    private final Long paymentId;
    private final String idempotencyKey;
    private final String customerId;
    private final long amount;
    private final PaymentStatus status;
    private final PaymentFailureReason failureReason;
    private final long accumulatedCancelAmount;
    private final long remainingCancelableAmount;
    private final LocalDateTime createdAt;

    /** Payment 엔티티를 불변 Application 결과로 변환한다. */
    public static PaymentResult from(Payment payment) {
        return PaymentResult.builder().paymentId(payment.getId()).idempotencyKey(payment.getIdempotencyKey())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount()).status(payment.getStatus()).failureReason(payment.getFailureReason())
                .accumulatedCancelAmount(payment.getAccumulatedCancelAmount())
                .remainingCancelableAmount(payment.remainingCancelableAmount())
                .createdAt(payment.getCreatedAt()).build();
    }
}
