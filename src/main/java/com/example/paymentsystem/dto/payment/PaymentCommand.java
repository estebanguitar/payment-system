package com.example.paymentsystem.dto.payment;

import lombok.Builder;
import lombok.Getter;

/** 신규 결제 요청에 필요한 고객·금액·멱등키를 전달한다. */
@Getter
@Builder
public class PaymentCommand {
    private final String customerId;
    private final long amount;
    private final String idempotencyKey;
}
