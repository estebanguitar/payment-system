package com.example.paymentsystem.audit.domain;

import com.example.paymentsystem.shared.domain.exception.DomainErrorCode;
import com.example.paymentsystem.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 변경 불가능한 클라이언트 HTTP 요청 감사 정보를 보관한다. */
@Getter
@Entity
@Table(name = "audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64)
    private String traceId;
    @Column(nullable = false, length = 100)
    private String clientId;
    @Column(nullable = false, length = 10)
    private String httpMethod;
    @Column(nullable = false, length = 255)
    private String requestPath;
    @Column(nullable = false)
    private int responseStatus;
    @Column(length = 64)
    private String errorCode;
    @Column(nullable = false)
    private boolean success;
    @Column(nullable = false)
    private long durationMs;
    @Column(nullable = false)
    private LocalDateTime requestedAt;
    @Column(nullable = false)
    private LocalDateTime completedAt;

    private AuditLog(String traceId, String clientId, String httpMethod, String requestPath, int responseStatus,
            String errorCode, long durationMs, LocalDateTime requestedAt, LocalDateTime completedAt) {
        requireText(traceId, 64, "추적 식별자");
        requireText(clientId, 100, "클라이언트 식별자");
        requireText(httpMethod, 10, "HTTP 메서드");
        requireText(requestPath, 255, "요청 경로");
        if (responseStatus < 100 || responseStatus > 599 || durationMs < 0 || requestedAt == null
                || completedAt == null || completedAt.isBefore(requestedAt)) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "감사 로그 처리 정보가 올바르지 않습니다.");
        }
        this.traceId = traceId;
        this.clientId = clientId;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.responseStatus = responseStatus;
        this.errorCode = errorCode;
        this.success = responseStatus < 400;
        this.durationMs = durationMs;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
    }

    /** 허용된 요청 메타데이터만 사용해 추가 전용 감사 로그를 생성한다. */
    public static AuditLog create(String traceId, String clientId, String httpMethod, String requestPath,
            int responseStatus, String errorCode, long durationMs, LocalDateTime requestedAt,
            LocalDateTime completedAt) {
        return new AuditLog(traceId, clientId, httpMethod, requestPath, responseStatus, errorCode, durationMs,
                requestedAt, completedAt);
    }

    private static void requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, field + "가 올바르지 않습니다.");
        }
    }
}
