package com.example.paymentsystem.domain.outbox;

import com.example.paymentsystem.domain.exception.DomainErrorCode;
import com.example.paymentsystem.domain.exception.DomainException;
import com.example.paymentsystem.domain.exception.InvalidPaymentStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 결제 요청 이벤트의 발행 상태와 복구 정보를 관리하는 아웃박스 원장이다. */
@Getter
@Entity
@Table(name = "payment_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private OutboxEventType eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private PaymentOutbox(Long paymentId, String idempotencyKey, String payload, LocalDateTime now) {
        requireId(paymentId);
        this.idempotencyKey = requireText(idempotencyKey, "결제 멱등키는 필수입니다.");
        this.payload = requireText(payload, "아웃박스 payload는 필수입니다.");
        requireNow(now);
        this.paymentId = paymentId;
        this.eventType = OutboxEventType.PAYMENT_REQUESTED;
        this.status = OutboxStatus.INIT;
        this.retryCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 결제 요청 이벤트를 아직 발행되지 않은 INIT 상태로 생성한다. */
    public static PaymentOutbox createPaymentRequested(Long paymentId, String idempotencyKey,
                                                       String payload, LocalDateTime now) {
        return new PaymentOutbox(paymentId, idempotencyKey, payload, now);
    }

    /** 발행 대기 중인 이벤트를 정상 발행 완료 상태로 전환한다. */
    public void markPublished(LocalDateTime now) {
        requireInit();
        requireNow(now);
        status = OutboxStatus.PUBLISHED;
        updatedAt = now;
    }

    /** 발행 실패 횟수를 증가시키되 재시도 가능한 INIT 상태를 유지한다. */
    public void recordFailure(LocalDateTime now) {
        requireInit();
        requireNow(now);
        try {
            retryCount = Math.incrementExact(retryCount);
        } catch (ArithmeticException exception) {
            throw new DomainException(DomainErrorCode.OUTBOX_RETRY_OVERFLOW);
        }
        updatedAt = now;
    }

    /** 재시도 정책이 종료된 발행 대기 이벤트를 최종 실패 상태로 전환한다. */
    public void markFailed(LocalDateTime now) {
        requireInit();
        requireNow(now);
        status = OutboxStatus.FAILED;
        updatedAt = now;
    }

    private void requireInit() {
        if (status != OutboxStatus.INIT) {
            throw new InvalidPaymentStateException();
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
