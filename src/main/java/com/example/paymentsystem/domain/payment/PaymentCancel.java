package com.example.paymentsystem.domain.payment;

import com.example.paymentsystem.common.exception.DomainErrorCode;
import com.example.paymentsystem.common.exception.DomainException;
import com.example.paymentsystem.common.exception.InvalidCancelAmountException;
import com.example.paymentsystem.common.exception.InvalidPaymentStateException;
import com.example.paymentsystem.domain.payment.PaymentFailureReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 원 결제와 분리하여 각 취소 시도의 금액, 상태 및 실패 사유를 관리한다. */
@Getter
@Entity
@Table(name = "payment_cancel")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCancel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_type", nullable = false, length = 32)
    private CancelType cancelType;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CancelStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 64)
    private PaymentFailureReason failureReason;

    @Column(name = "pg_cancel_transaction_id", length = 128)
    private String pgCancelTransactionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private PaymentCancel(Long paymentId, String idempotencyKey, CancelType cancelType,
                          long requestedAmount, long remainingAmount, LocalDateTime now) {
        requireId(paymentId);
        this.idempotencyKey = requireText(idempotencyKey, "취소 멱등키는 필수입니다.");
        if (cancelType == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "취소 유형은 필수입니다.");
        }
        validateAmount(cancelType, requestedAmount, remainingAmount);
        requireNow(now);
        this.paymentId = paymentId;
        this.cancelType = cancelType;
        this.amount = requestedAmount;
        this.status = CancelStatus.PENDING;
        this.createdAt = now;
    }

    /** 원 결제의 잔여 금액과 취소 유형을 검증하고 PENDING 취소를 생성한다. */
    public static PaymentCancel createPending(Long paymentId, String idempotencyKey,
                                              CancelType cancelType, long requestedAmount,
                                              long remainingAmount, LocalDateTime now) {
        return new PaymentCancel(paymentId, idempotencyKey, cancelType,
                requestedAmount, remainingAmount, now);
    }

    /** 외부 PG 취소가 승인된 PENDING 취소를 완료 처리한다. */
    public void complete(String pgCancelTransactionId, LocalDateTime now) {
        requirePending();
        this.pgCancelTransactionId = requireText(pgCancelTransactionId, "PG 취소 거래 식별자는 필수입니다.");
        requireNow(now);
        status = CancelStatus.COMPLETED;
        failureReason = null;
        completedAt = now;
    }

    /** 실패한 PENDING 취소에 사유와 완료 시각을 기록한다. */
    public void fail(PaymentFailureReason reason, LocalDateTime now) {
        requirePending();
        if (reason == null) {
            throw new DomainException(DomainErrorCode.MISSING_FAILURE_REASON);
        }
        requireNow(now);
        status = CancelStatus.FAILED;
        failureReason = reason;
        completedAt = now;
    }

    private void requirePending() {
        if (status != CancelStatus.PENDING) {
            throw new InvalidPaymentStateException();
        }
    }

    private static void validateAmount(CancelType type, long requestedAmount, long remainingAmount) {
        if (remainingAmount <= 0 || requestedAmount <= 0) {
            throw new InvalidCancelAmountException();
        }
        if (type == CancelType.FULL && requestedAmount != remainingAmount) {
            throw new InvalidCancelAmountException();
        }
        if (type == CancelType.PARTIAL && requestedAmount >= remainingAmount) {
            throw new InvalidCancelAmountException();
        }
    }

    private static void requireId(Long id) {
        if (id == null || id <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "결제 식별자는 필수입니다.");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, message);
        }
        return value;
    }

    private static void requireNow(LocalDateTime now) {
        if (now == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "현재 시각은 필수입니다.");
        }
    }
}
