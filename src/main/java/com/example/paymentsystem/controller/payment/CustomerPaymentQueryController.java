package com.example.paymentsystem.controller.payment;

import com.example.paymentsystem.service.payment.PaymentQueryService;
import com.example.paymentsystem.common.response.ApiResponse;
import com.example.paymentsystem.dto.payment.PaymentResponse;
import com.example.paymentsystem.dto.payment.CustomerPaymentResponse;
import com.example.paymentsystem.dto.payment.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 고객 소유권 범위에서 결제 목록과 상세 조회를 제공한다. */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Customer Payment Query", description = "고객 결제 조회 API")
public class CustomerPaymentQueryController {
    private final PaymentQueryService paymentQueryService;

    /** 고객의 결제 목록을 최신순 1-based 페이지로 조회한다. */
    @GetMapping
    @Operation(summary = "고객 결제 목록 조회")
    public ApiResponse<PageResponse<PaymentResponse>> list(@PathVariable String customerId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(PageResponse.from(paymentQueryService.getCustomerPayments(customerId, page, size),
                PaymentResponse::from));
    }

    /** 고객 식별자와 결제 식별자를 함께 사용해 소유권이 확인된 상세를 조회한다. */
    @GetMapping("/{paymentId}")
    @Operation(summary = "고객 결제 상세 조회")
    public ApiResponse<CustomerPaymentResponse> get(@PathVariable String customerId, @PathVariable Long paymentId) {
        return ApiResponse.success(CustomerPaymentResponse.from(
                paymentQueryService.getCustomerPayment(customerId, paymentId)));
    }
}
