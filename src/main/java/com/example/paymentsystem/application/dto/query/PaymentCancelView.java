package com.example.paymentsystem.application.dto.query;

import com.example.paymentsystem.domain.entity.cancel.CancelStatus;
import com.example.paymentsystem.domain.entity.cancel.CancelType;
import com.example.paymentsystem.domain.entity.cancel.PaymentCancel;
import com.example.paymentsystem.domain.entity.payment.PaymentFailureReason;
import java.time.LocalDateTime;

/** 조회 응답에 필요한 취소 상태와 금액만 전달한다. */
public record PaymentCancelView(Long cancelId, CancelType type, long amount, CancelStatus status,
                                PaymentFailureReason failureReason, LocalDateTime createdAt) {
    /** 취소 엔티티를 안전한 조회 DTO로 변환한다. */
    public static PaymentCancelView from(PaymentCancel cancel) {
        return new PaymentCancelView(cancel.getId(), cancel.getCancelType(), cancel.getAmount(),
                cancel.getStatus(), cancel.getFailureReason(), cancel.getCreatedAt());
    }
}
