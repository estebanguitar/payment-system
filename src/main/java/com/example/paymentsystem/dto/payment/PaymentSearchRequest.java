package com.example.paymentsystem.dto.payment;

import com.example.paymentsystem.dto.payment.query.PaymentSearchCondition;
import com.example.paymentsystem.domain.payment.PaymentFailureReason;
import com.example.paymentsystem.domain.payment.PaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** 운영 결제 검색 쿼리와 페이지 범위를 검증한다. */
@Getter
@Setter
public class PaymentSearchRequest {
    private Long paymentId;
    private String customerId;
    private PaymentStatus status;
    private PaymentFailureReason failureReason;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    @Min(1)
    private int page = 1;
    @Min(1) @Max(100)
    private int size = 20;

    /** 검증된 HTTP 검색 조건을 애플리케이션 조회 조건으로 변환한다. */
    public PaymentSearchCondition toCondition() {
        return PaymentSearchCondition.builder().paymentId(paymentId).customerId(customerId).status(status)
                .failureReason(failureReason).startDate(startDate).endDate(endDate).page(page).size(size).build();
    }
}
