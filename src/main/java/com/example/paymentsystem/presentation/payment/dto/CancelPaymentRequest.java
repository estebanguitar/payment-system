package com.example.paymentsystem.presentation.payment.dto;

import com.example.paymentsystem.application.dto.cancel.PaymentCancelCommand;
import com.example.paymentsystem.domain.entity.cancel.CancelType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 결제 취소 유형과 금액을 검증한다. */
@Getter
@NoArgsConstructor
public class CancelPaymentRequest {
    @NotNull
    private CancelType cancelType;
    @Positive
    private long amount;

    /** 경로와 헤더 값을 결합해 결제 취소 명령을 생성한다. */
    public PaymentCancelCommand toCommand(Long paymentId, String idempotencyKey) {
        return PaymentCancelCommand.builder().paymentId(paymentId).idempotencyKey(idempotencyKey)
                .cancelType(cancelType).amount(amount).build();
    }
}
