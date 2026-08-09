package com.example.paymentsystem.payment.presentation.dto;

import com.example.paymentsystem.payment.application.dto.PaymentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 결제 생성 입력을 검증하고 애플리케이션 명령으로 변환한다. */
@Getter
@NoArgsConstructor
public class CreatePaymentRequest {
    @NotBlank @Size(max = 64)
    private String customerId;
    @Positive
    private long amount;

    /** 헤더의 멱등키를 포함한 결제 명령을 생성한다. */
    public PaymentCommand toCommand(String idempotencyKey) {
        return PaymentCommand.builder().customerId(customerId).amount(amount)
                .idempotencyKey(idempotencyKey).build();
    }
}
