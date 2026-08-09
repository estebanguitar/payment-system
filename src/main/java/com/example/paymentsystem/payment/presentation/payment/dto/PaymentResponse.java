package com.example.paymentsystem.payment.presentation.dto;

import com.example.paymentsystem.payment.application.dto.PaymentResult;
import com.example.paymentsystem.payment.domain.PaymentFailureReason;
import com.example.paymentsystem.payment.domain.PaymentStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 결제 처리 및 목록 조회에 사용하는 외부 응답이다. */
@Getter
@Builder
public class PaymentResponse {
    private final Long paymentId;
    private final String customerId;
    private final long amount;
    private final PaymentStatus status;
    private final PaymentFailureReason failureReason;
    private final long accumulatedCancelAmount;
    private final long remainingCancelableAmount;
    private final LocalDateTime createdAt;

    /** 애플리케이션 결제 결과를 API 응답으로 변환한다. */
    public static PaymentResponse from(PaymentResult source) {
        return PaymentResponse.builder().paymentId(source.getPaymentId()).customerId(source.getCustomerId())
                .amount(source.getAmount()).status(source.getStatus())
                .failureReason(source.getFailureReason()).accumulatedCancelAmount(source.getAccumulatedCancelAmount())
                .remainingCancelableAmount(source.getRemainingCancelableAmount()).createdAt(source.getCreatedAt()).build();
    }
}
