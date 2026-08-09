package com.example.paymentsystem.application.dto.query;

import com.example.paymentsystem.application.dto.payment.PaymentResult;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 운영 확인용 결제와 연관 리소스의 메타데이터를 제공한다. */
@Getter
@Builder
public class OperationsPaymentView {
    private final PaymentResult payment;
    private final List<PaymentCancelView> cancellations;
    private final List<WalletTransactionView> walletTransactions;
    private final List<Long> pgResponseLogIds;
}
