package com.example.paymentsystem.audit.application.dto;

import com.example.paymentsystem.audit.domain.AuditLog;
import java.time.LocalDateTime;

/** 민감 원문 없이 운영 조회에 필요한 감사 정보만 제공한다. */
public record AuditLogResult(Long auditLogId, String traceId, String clientId, String httpMethod,
        String requestPath, int responseStatus, String errorCode, boolean success, long durationMs,
        LocalDateTime requestedAt, LocalDateTime completedAt) {
    /** 감사 엔티티를 안전한 조회 결과로 변환한다. */
    public static AuditLogResult from(AuditLog source) {
        return new AuditLogResult(source.getId(), source.getTraceId(), source.getClientId(), source.getHttpMethod(),
                source.getRequestPath(), source.getResponseStatus(), source.getErrorCode(), source.isSuccess(),
                source.getDurationMs(), source.getRequestedAt(), source.getCompletedAt());
    }
}
