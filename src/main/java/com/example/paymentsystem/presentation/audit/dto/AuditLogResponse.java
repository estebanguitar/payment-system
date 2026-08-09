package com.example.paymentsystem.presentation.audit.dto;

import com.example.paymentsystem.application.dto.audit.AuditLogResult;
import java.time.LocalDateTime;

/** 운영 API에서 공개할 감사 로그 메타데이터를 표현한다. */
public record AuditLogResponse(Long auditLogId, String traceId, String clientId, String httpMethod,
        String requestPath, int responseStatus, String errorCode, boolean success, long durationMs,
        LocalDateTime requestedAt, LocalDateTime completedAt) {
    /** 애플리케이션 감사 결과를 API 응답으로 변환한다. */
    public static AuditLogResponse from(AuditLogResult source) {
        return new AuditLogResponse(source.auditLogId(), source.traceId(), source.clientId(), source.httpMethod(),
                source.requestPath(), source.responseStatus(), source.errorCode(), source.success(),
                source.durationMs(), source.requestedAt(), source.completedAt());
    }
}
