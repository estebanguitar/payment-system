package com.example.paymentsystem.dto.cancellation;

import com.example.paymentsystem.domain.cancellation.CancelType;
import lombok.Builder;
import lombok.Getter;

/** 원 결제의 전액 또는 부분 취소 요청을 전달한다. */
@Getter
@Builder
public class PaymentCancelCommand {
    private final Long paymentId;
    private final String idempotencyKey;
    private final CancelType cancelType;
    private final long amount;
}
