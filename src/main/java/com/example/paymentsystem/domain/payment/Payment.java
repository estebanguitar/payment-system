package com.example.paymentsystem.domain.payment;

import com.example.paymentsystem.common.exception.DomainErrorCode;
import com.example.paymentsystem.common.exception.DomainException;
import com.example.paymentsystem.common.exception.InvalidAmountException;
import com.example.paymentsystem.common.exception.InvalidCancelAmountException;
import com.example.paymentsystem.common.exception.InvalidPaymentStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 결제 상태, 실패 사유 및 누적 취소 금액의 불변식을 관리하는 Aggregate다. */
@Getter
@Entity
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 64)
    private PaymentFailureReason failureReason;

    @Column(name = "accumulated_cancel_amount", nullable = false)
    private long accumulatedCancelAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Payment(String idempotencyKey, String customerId, long amount, LocalDateTime now) {
        this.idempotencyKey = requireText(idempotencyKey, "결제 멱등키는 필수입니다.");
        this.customerId = requireText(customerId, "고객 식별자는 필수입니다.");
        requirePositive(amount);
        requireNow(now);
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.accumulatedCancelAmount = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 검증된 결제 요청을 외부 처리 전 PENDING 상태로 생성한다. */
    public static Payment createPending(String idempotencyKey, String customerId,
                                        long amount, LocalDateTime now) {
        return new Payment(idempotencyKey, customerId, amount, now);
    }

    /** 외부 승인과 잔액 차감이 성공한 PENDING 결제를 완료 처리한다. */
    public void complete(LocalDateTime now) {
        requireStatus(PaymentStatus.PENDING, "대기 중인 결제만 완료할 수 있습니다.");
        requireNow(now);
        status = PaymentStatus.COMPLETED;
        failureReason = null;
        updatedAt = now;
    }

    /** 처리하지 못한 PENDING 결제를 지정한 실패 사유와 함께 종료한다. */
    public void fail(PaymentFailureReason reason, LocalDateTime now) {
        requireStatus(PaymentStatus.PENDING, "대기 중인 결제만 실패 처리할 수 있습니다.");
        if (reason == null) {
            throw new DomainException(DomainErrorCode.MISSING_FAILURE_REASON);
        }
        requireNow(now);
        status = PaymentStatus.FAILED;
        failureReason = reason;
        updatedAt = now;
    }

    /** 취소 금액을 누적하고 잔여 금액에 따라 부분 또는 전액 취소 상태로 전환한다. */
    public void applyCancellation(long cancelAmount, LocalDateTime now) {
        if (!isCancelable()) {
            throw new InvalidPaymentStateException();
        }
        if (cancelAmount <= 0 || cancelAmount > remainingCancelableAmount()) {
            throw new InvalidCancelAmountException();
        }
        requireNow(now);
        try {
            accumulatedCancelAmount = Math.addExact(accumulatedCancelAmount, cancelAmount);
        } catch (ArithmeticException exception) {
            throw new InvalidCancelAmountException();
        }
        status = accumulatedCancelAmount == amount
                ? PaymentStatus.CANCELED
                : PaymentStatus.PARTIALLY_CANCELED;
        failureReason = null;
        updatedAt = now;
    }

    /** 원 결제 금액에서 완료된 누적 취소 금액을 뺀 취소 가능 금액을 반환한다. */
    public long remainingCancelableAmount() {
        return amount - accumulatedCancelAmount;
    }

    /** 현재 결제 상태가 취소 가능한지 확인한다. */
    public boolean isCancelable() {
        return status == PaymentStatus.COMPLETED || status == PaymentStatus.PARTIALLY_CANCELED;
    }

    /** 멱등 재요청의 고객과 금액이 최초 요청과 같은지 확인한다. */
    public boolean matchesRequest(String customerId, long amount) {
        return Objects.equals(this.customerId, customerId) && this.amount == amount;
    }

    private void requireStatus(PaymentStatus expected, String message) {
        if (status != expected) {
            throw new InvalidPaymentStateException(message);
        }
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException();
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
