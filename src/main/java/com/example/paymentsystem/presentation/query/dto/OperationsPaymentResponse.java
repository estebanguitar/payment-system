package com.example.paymentsystem.presentation.query.dto;

import com.example.paymentsystem.application.dto.query.OperationsPaymentView;
import com.example.paymentsystem.presentation.payment.dto.PaymentResponse;
import java.util.List;

/** 운영자 분석에 필요한 결제 연관 데이터를 조합한다. */
public record OperationsPaymentResponse(PaymentResponse payment, List<PaymentCancelItemResponse> cancellations,
        List<WalletTransactionItemResponse> walletTransactions, List<Long> pgResponseLogIds) {
    /** 운영 결제 조회 결과를 복합 API 응답으로 변환한다. */
    public static OperationsPaymentResponse from(OperationsPaymentView source) {
        return new OperationsPaymentResponse(PaymentResponse.from(source.getPayment()),
                source.getCancellations().stream().map(PaymentCancelItemResponse::from).toList(),
                source.getWalletTransactions().stream().map(WalletTransactionItemResponse::from).toList(),
                source.getPgResponseLogIds());
    }
}
