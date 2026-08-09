package com.example.paymentsystem.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentsystem.cancellation.application.dto.PaymentCancelCommand;
import com.example.paymentsystem.cancellation.application.dto.PaymentCancelResult;
import com.example.paymentsystem.payment.application.dto.PaymentCommand;
import com.example.paymentsystem.payment.application.dto.PaymentResult;
import com.example.paymentsystem.wallet.application.dto.WalletCommand;
import com.example.paymentsystem.cancellation.application.service.PaymentCancelFacade;
import com.example.paymentsystem.payment.application.service.PaymentFacade;
import com.example.paymentsystem.wallet.application.service.WalletService;
import com.example.paymentsystem.cancellation.domain.CancelStatus;
import com.example.paymentsystem.cancellation.domain.CancelType;
import com.example.paymentsystem.customer.domain.Customer;
import com.example.paymentsystem.payment.domain.PaymentStatus;
import com.example.paymentsystem.customer.infrastructure.repository.CustomerRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 애플리케이션 서비스의 충전·결제·취소 전체 흐름과 최종 상태를 검증한다. */
@SpringBootTest(properties = "payment.security.encryption-key="
        + "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class PaymentApplicationIntegrationTest {
    @Autowired private CustomerRepository customerRepository;
    @Autowired private WalletService walletService;
    @Autowired private PaymentFacade paymentFacade;
    @Autowired private PaymentCancelFacade cancelFacade;

    /** 커밋 후 PG 승인으로 잔액이 차감되고 전액 취소 시 다시 환불되는지 확인한다. */
    @Test
    void completePaymentAndFullCancellation() {
        String suffix = UUID.randomUUID().toString();
        String customerId = "CUST-" + suffix;
        customerRepository.save(Customer.create(customerId, "테스트 고객", null, LocalDateTime.now()));
        walletService.createWallet(customerId);
        walletService.topUp(WalletCommand.TopUp.builder().customerId(customerId).amount(10_000)
                .idempotencyKey("TOPUP-" + suffix).build());

        PaymentResult payment = paymentFacade.requestPayment(PaymentCommand.builder()
                .customerId(customerId).amount(6_000).idempotencyKey("PAY-" + suffix).build());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(walletService.getWallet(customerId).getBalance()).isEqualTo(4_000);

        PaymentCancelResult cancel = cancelFacade.cancel(PaymentCancelCommand.builder()
                .paymentId(payment.getPaymentId()).cancelType(CancelType.FULL).amount(6_000)
                .idempotencyKey("CANCEL-" + suffix).build());

        assertThat(cancel.getCancelStatus()).isEqualTo(CancelStatus.COMPLETED);
        assertThat(cancel.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(walletService.getWallet(customerId).getBalance()).isEqualTo(10_000);
    }
}
