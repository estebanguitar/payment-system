package com.example.paymentsystem.dto.payment.query;

import com.example.paymentsystem.domain.payment.PaymentFailureReason;
import com.example.paymentsystem.domain.payment.PaymentStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 운영자 결제 검색 조건과 1-based 페이징 값을 전달한다. */
@Getter
@Builder
public class PaymentSearchCondition {
    private final Long paymentId;
    private final String customerId;
    private final PaymentStatus status;
    private final PaymentFailureReason failureReason;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final int page;
    private final int size;
}
