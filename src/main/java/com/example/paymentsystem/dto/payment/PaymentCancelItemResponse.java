package com.example.paymentsystem.dto.payment;

import com.example.paymentsystem.dto.payment.query.PaymentCancelView;
import com.example.paymentsystem.domain.cancellation.CancelStatus;
import com.example.paymentsystem.domain.cancellation.CancelType;
import com.example.paymentsystem.domain.payment.PaymentFailureReason;
import java.time.LocalDateTime;

/** 조회 응답에 포함되는 취소 이력 항목이다. */
public record PaymentCancelItemResponse(Long cancelId, CancelType type, long amount, CancelStatus status,
        PaymentFailureReason failureReason, LocalDateTime createdAt) {
    /** 애플리케이션 취소 조회 결과를 API 항목으로 변환한다. */
    public static PaymentCancelItemResponse from(PaymentCancelView source) {
        return new PaymentCancelItemResponse(source.cancelId(), source.type(), source.amount(), source.status(),
                source.failureReason(), source.createdAt());
    }
}
