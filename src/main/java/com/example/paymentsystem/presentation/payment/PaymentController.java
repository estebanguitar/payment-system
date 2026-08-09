package com.example.paymentsystem.presentation.payment;

import com.example.paymentsystem.application.service.cancel.PaymentCancelFacade;
import com.example.paymentsystem.application.service.payment.PaymentFacade;
import com.example.paymentsystem.shared.presentation.ApiResponse;
import com.example.paymentsystem.presentation.payment.dto.CancelPaymentRequest;
import com.example.paymentsystem.presentation.payment.dto.CreatePaymentRequest;
import com.example.paymentsystem.presentation.payment.dto.PaymentCancelResponse;
import com.example.paymentsystem.presentation.payment.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 결제 생성과 취소 HTTP 요청을 각 유스케이스에 연결한다. */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "결제 API")
public class PaymentController {
    private final PaymentFacade paymentFacade;
    private final PaymentCancelFacade paymentCancelFacade;

    /** 멱등키를 적용해 결제를 생성하고 최종 처리 결과를 반환한다. */
    @PostMapping
    @Operation(summary = "결제 생성")
    public ApiResponse<PaymentResponse> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.success(PaymentResponse.from(paymentFacade.requestPayment(request.toCommand(idempotencyKey))));
    }

    /** 전액 또는 부분 취소를 수행하고 누적 취소 상태를 반환한다. */
    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "결제 취소")
    public ApiResponse<PaymentCancelResponse> cancel(@PathVariable Long paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CancelPaymentRequest request) {
        return ApiResponse.success(PaymentCancelResponse.from(
                paymentCancelFacade.cancel(request.toCommand(paymentId, idempotencyKey))));
    }
}
