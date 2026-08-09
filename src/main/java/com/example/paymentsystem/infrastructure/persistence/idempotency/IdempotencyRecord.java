package com.example.paymentsystem.infrastructure.persistence.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 업무 테이블을 넘어 멱등키를 원자적으로 선점하고 처리 결과를 연결한다. */
@Getter
@Entity
@Table(name = "idempotency_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 32)
    private IdempotencyRequestType requestType;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private IdempotencyRecord(String idempotencyKey, IdempotencyRequestType requestType,
                              String requestFingerprint, LocalDateTime now) {
        this.idempotencyKey = requireText(idempotencyKey, "멱등키는 필수입니다.");
        if (requestType == null) {
            throw new IllegalArgumentException("멱등 요청 유형은 필수입니다.");
        }
        this.requestFingerprint = requireFingerprint(requestFingerprint);
        if (now == null) {
            throw new IllegalArgumentException("멱등키 선점 시각은 필수입니다.");
        }
        this.requestType = requestType;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 결과 연결 전 상태로 시스템 전체 멱등키를 선점한다. */
    public static IdempotencyRecord reserve(String idempotencyKey, IdempotencyRequestType requestType,
                                            String requestFingerprint, LocalDateTime now) {
        return new IdempotencyRecord(idempotencyKey, requestType, requestFingerprint, now);
    }

    /** 최초 요청의 유형과 지문이 재요청 데이터와 동일한지 확인한다. */
    public boolean matches(IdempotencyRequestType requestType, String requestFingerprint) {
        return this.requestType == requestType && this.requestFingerprint.equals(requestFingerprint);
    }

    /** 멱등키로 생성된 업무 리소스를 한 번만 연결한다. */
    public void linkResource(Long resourceId, LocalDateTime now) {
        if (this.resourceId != null) {
            throw new IllegalStateException("멱등 처리 결과가 이미 연결되어 있습니다.");
        }
        if (resourceId == null || resourceId <= 0 || now == null) {
            throw new IllegalArgumentException("유효한 리소스 식별자와 처리 시각이 필요합니다.");
        }
        this.resourceId = resourceId;
        this.updatedAt = now;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireFingerprint(String fingerprint) {
        String value = requireText(fingerprint, "요청 지문은 필수입니다.");
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("요청 지문은 소문자 SHA-256 형식이어야 합니다.");
        }
        return value;
    }
}
