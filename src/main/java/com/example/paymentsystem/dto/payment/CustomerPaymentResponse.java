package com.example.paymentsystem.dto.payment;

import com.example.paymentsystem.dto.payment.query.CustomerPaymentView;
import com.example.paymentsystem.dto.payment.PaymentResponse;
import java.util.List;

/** 고객에게 허용된 결제 정보와 취소 이력만 표현한다. */
public record CustomerPaymentResponse(PaymentResponse payment, List<PaymentCancelItemResponse> cancellations) {
    /** 고객 결제 조회 결과를 외부 노출 범위로 변환한다. */
    public static CustomerPaymentResponse from(CustomerPaymentView source) {
        return new CustomerPaymentResponse(PaymentResponse.from(source.getPayment()),
                source.getCancellations().stream().map(PaymentCancelItemResponse::from).toList());
    }
}
