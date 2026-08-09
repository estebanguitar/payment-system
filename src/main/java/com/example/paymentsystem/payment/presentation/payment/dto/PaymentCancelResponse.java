package com.example.paymentsystem.payment.presentation.dto;

import com.example.paymentsystem.cancellation.application.dto.PaymentCancelResult;
import com.example.paymentsystem.cancellation.domain.CancelStatus;
import com.example.paymentsystem.cancellation.domain.CancelType;
import com.example.paymentsystem.payment.domain.PaymentFailureReason;
import com.example.paymentsystem.payment.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

/** 결제 취소 시도와 결제의 최종 상태를 함께 표현한다. */
@Getter
@Builder
public class PaymentCancelResponse {
    private final Long cancelId;
    private final Long paymentId;
    private final long cancelAmount;
    private final CancelType cancelType;
    private final CancelStatus cancelStatus;
    private final PaymentFailureReason failureReason;
    private final PaymentStatus paymentStatus;
    private final long accumulatedCancelAmount;
    private final long remainingCancelableAmount;

    /** 애플리케이션 취소 결과를 API 응답으로 변환한다. */
    public static PaymentCancelResponse from(PaymentCancelResult source) {
        return PaymentCancelResponse.builder().cancelId(source.getCancelId()).paymentId(source.getPaymentId())
                .cancelAmount(source.getCancelAmount()).cancelType(source.getCancelType())
                .cancelStatus(source.getCancelStatus()).failureReason(source.getFailureReason())
                .paymentStatus(source.getPaymentStatus()).accumulatedCancelAmount(source.getAccumulatedCancelAmount())
                .remainingCancelableAmount(source.getRemainingCancelableAmount()).build();
    }
}
