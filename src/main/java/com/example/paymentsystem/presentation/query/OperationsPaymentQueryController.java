package com.example.paymentsystem.presentation.query;

import com.example.paymentsystem.application.service.query.PaymentQueryService;
import com.example.paymentsystem.presentation.common.ApiResponse;
import com.example.paymentsystem.presentation.payment.dto.PaymentResponse;
import com.example.paymentsystem.presentation.query.dto.OperationsPaymentResponse;
import com.example.paymentsystem.presentation.query.dto.PageResponse;
import com.example.paymentsystem.presentation.query.dto.PaymentSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영자 결제 검색과 연관 데이터 상세 조회를 제공한다. */
@RestController
@RequestMapping("/api/v1/ops/payments")
@RequiredArgsConstructor
@Tag(name = "Operations Payment Query", description = "운영자 결제 조회 API")
public class OperationsPaymentQueryController {
    private final PaymentQueryService paymentQueryService;

    /** 여러 선택 조건으로 결제를 최신순 검색한다. */
    @GetMapping
    @Operation(summary = "운영 결제 검색")
    public ApiResponse<PageResponse<PaymentResponse>> search(@Valid @ModelAttribute PaymentSearchRequest request) {
        return ApiResponse.success(PageResponse.from(paymentQueryService.search(request.toCondition()),
                PaymentResponse::from));
    }

    /** 결제·취소·지갑 거래·PG 로그 식별자를 조합해 운영 상세를 조회한다. */
    @GetMapping("/{paymentId}")
    @Operation(summary = "운영 결제 상세 조회")
    public ApiResponse<OperationsPaymentResponse> get(@PathVariable Long paymentId) {
        return ApiResponse.success(OperationsPaymentResponse.from(
                paymentQueryService.getOperationsPayment(paymentId)));
    }
}
