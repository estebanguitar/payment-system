package com.example.paymentsystem.cancellation.application.dto;

import com.example.paymentsystem.cancellation.domain.PaymentCancel;
import com.example.paymentsystem.cancellation.domain.CancelStatus;
import com.example.paymentsystem.cancellation.domain.CancelType;
import com.example.paymentsystem.payment.domain.Payment;
import com.example.paymentsystem.payment.domain.PaymentFailureReason;
import com.example.paymentsystem.payment.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

/** 결제 취소 시도와 원 결제의 누적 취소 결과를 함께 반환한다. */
@Getter
@Builder
public class PaymentCancelResult {
    private final Long cancelId;
    private final Long paymentId;
    private final long cancelAmount;
    private final CancelType cancelType;
    private final CancelStatus cancelStatus;
    private final PaymentFailureReason failureReason;
    private final PaymentStatus paymentStatus;
    private final long accumulatedCancelAmount;
    private final long remainingCancelableAmount;

    /** 취소와 원 결제 엔티티를 하나의 Application 결과로 변환한다. */
    public static PaymentCancelResult from(PaymentCancel cancel, Payment payment) {
        return PaymentCancelResult.builder().cancelId(cancel.getId()).paymentId(payment.getId())
                .cancelAmount(cancel.getAmount()).cancelType(cancel.getCancelType()).cancelStatus(cancel.getStatus())
                .failureReason(cancel.getFailureReason()).paymentStatus(payment.getStatus())
                .accumulatedCancelAmount(payment.getAccumulatedCancelAmount())
                .remainingCancelableAmount(payment.remainingCancelableAmount()).build();
    }
}
