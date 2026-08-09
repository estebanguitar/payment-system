package com.example.paymentsystem.architecture.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.paymentsystem.dto.payment.PaymentCancelResult;
import com.example.paymentsystem.dto.payment.PaymentResult;
import com.example.paymentsystem.dto.payment.query.CustomerPaymentView;
import com.example.paymentsystem.dto.payment.query.OperationsPaymentView;
import com.example.paymentsystem.dto.common.PageResult;
import com.example.paymentsystem.dto.wallet.WalletResult;
import com.example.paymentsystem.service.payment.PaymentCancelFacade;
import com.example.paymentsystem.service.audit.AuditLogService;
import com.example.paymentsystem.service.payment.PaymentFacade;
import com.example.paymentsystem.service.payment.PaymentQueryService;
import com.example.paymentsystem.service.wallet.WalletService;
import com.example.paymentsystem.domain.payment.CancelStatus;
import com.example.paymentsystem.domain.payment.CancelType;
import com.example.paymentsystem.domain.payment.PaymentStatus;
import com.example.paymentsystem.controller.payment.PaymentController;
import com.example.paymentsystem.common.audit.AuditValueSanitizer;
import com.example.paymentsystem.integration.pg.security.PayloadEncryptor;
import com.example.paymentsystem.controller.payment.CustomerPaymentQueryController;
import com.example.paymentsystem.controller.payment.OperationsPaymentQueryController;
import com.example.paymentsystem.controller.wallet.WalletController;
import java.util.List;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 프레젠테이션 엔드포인트의 바인딩, 상태 코드 및 공통 응답 계약을 검증한다. */
@WebMvcTest({WalletController.class, PaymentController.class, CustomerPaymentQueryController.class,
        OperationsPaymentQueryController.class})
@Import(PresentationControllerTest.MeterConfiguration.class)
class PresentationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private WalletService walletService;
    @MockitoBean
    private PaymentFacade paymentFacade;
    @MockitoBean
    private PaymentCancelFacade paymentCancelFacade;
    @MockitoBean
    private PaymentQueryService paymentQueryService;
    @MockitoBean
    private AuditLogService auditLogService;
    @MockitoBean
    private AuditValueSanitizer auditValueSanitizer;
    @MockitoBean
    private PayloadEncryptor payloadEncryptor;

    /** 지갑 생성·조회·충전 API가 서비스 결과를 표준 응답으로 반환하는지 검증한다. */
    @Test
    void walletEndpointsReturnStandardResponses() throws Exception {
        WalletResult.Balance balance = WalletResult.Balance.builder().walletId(1L).customerId("customer-1")
                .balance(1000).build();
        when(walletService.createWallet("customer-1")).thenReturn(balance);
        when(walletService.getWallet("customer-1")).thenReturn(balance);
        when(walletService.topUp(any())).thenReturn(WalletResult.TopUp.builder().transactionId(2L)
                .customerId("customer-1").amount(1000).balanceAfter(1000).build());

        mockMvc.perform(post("/api/v1/wallets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"customer-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
        mockMvc.perform(get("/api/v1/wallets/customer-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnObject.balance").value(1000));
        mockMvc.perform(post("/api/v1/wallets/customer-1/top-up").header("Idempotency-Key", "topup-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnObject.balanceAfter").value(1000));
    }

    /** 결제 생성·취소 API가 멱등 헤더와 요청 본문을 정상 수신하는지 검증한다. */
    @Test
    void paymentEndpointsReturnProcessingResults() throws Exception {
        when(paymentFacade.requestPayment(any())).thenReturn(payment());
        when(paymentCancelFacade.cancel(any())).thenReturn(PaymentCancelResult.builder().cancelId(3L)
                .paymentId(10L).cancelAmount(500).cancelType(CancelType.PARTIAL)
                .cancelStatus(CancelStatus.COMPLETED).paymentStatus(PaymentStatus.PARTIALLY_CANCELED)
                .remainingCancelableAmount(500).build());

        mockMvc.perform(post("/api/v1/payments").header("Idempotency-Key", "payment-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"customer-1\",\"amount\":1000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnObject.paymentId").value(10));
        mockMvc.perform(post("/api/v1/payments/10/cancel").header("Idempotency-Key", "cancel-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelType\":\"PARTIAL\",\"amount\":500}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnObject.cancelId").value(3));
    }

    /** 고객과 운영자 목록·상세 조회가 각 노출 범위에 맞는 응답을 반환하는지 검증한다. */
    @Test
    void queryEndpointsReturnPagedAndDetailedResponses() throws Exception {
        PageResult<PaymentResult> page = PageResult.<PaymentResult>builder().content(List.of(payment()))
                .page(1).size(20).totalElements(1).totalPages(1).build();
        when(paymentQueryService.getCustomerPayments("customer-1", 1, 20)).thenReturn(page);
        when(paymentQueryService.getCustomerPayment("customer-1", 10L)).thenReturn(
                CustomerPaymentView.builder().payment(payment()).cancellations(List.of()).build());
        when(paymentQueryService.search(any())).thenReturn(page);
        when(paymentQueryService.getOperationsPayment(10L)).thenReturn(OperationsPaymentView.builder()
                .payment(payment()).cancellations(List.of()).walletTransactions(List.of())
                .pgResponseLogIds(List.of(7L)).build());

        mockMvc.perform(get("/api/v1/customers/customer-1/payments"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnObject.totalElements").value(1));
        mockMvc.perform(get("/api/v1/customers/customer-1/payments/10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnObject.payment.paymentId").value(10));
        mockMvc.perform(get("/api/v1/ops/payments").param("status", "COMPLETED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnObject.content[0].paymentId").value(10));
        mockMvc.perform(get("/api/v1/ops/payments/10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnObject.pgResponseLogIds[0]").value(7));
    }

    /** 필수 헤더와 유효한 금액이 없으면 공통 400 오류를 반환하는지 검증한다. */
    @Test
    void invalidRequestReturnsStandardBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"amount\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUIRED_VALUE"));
    }

    private static PaymentResult payment() {
        return PaymentResult.builder().paymentId(10L).idempotencyKey("payment-1").customerId("customer-1")
                .amount(1000).status(PaymentStatus.COMPLETED).remainingCancelableAmount(1000).build();
    }

    /** Web MVC Slice에서 감사 실패 지표 등록에 필요한 메트릭 저장소를 제공한다. */
    @TestConfiguration
    static class MeterConfiguration {
        /** 테스트 범위의 인메모리 MeterRegistry를 생성한다. */
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
