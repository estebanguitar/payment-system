package com.example.paymentsystem.application.dto.query;

import com.example.paymentsystem.application.dto.payment.PaymentResult;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 고객에게 노출 가능한 결제 상태와 취소 이력만 제공한다. */
@Getter
@Builder
public class CustomerPaymentView {
    private final PaymentResult payment;
    private final List<PaymentCancelView> cancellations;
}
