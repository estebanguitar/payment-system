package com.example.paymentsystem.dto.audit;

import java.time.LocalDateTime;

/** HTTP 경계에서 허용된 감사 메타데이터만 저장 유스케이스에 전달한다. */
public record AuditLogCommand(String traceId, String clientId, String httpMethod, String requestPath,
        int responseStatus, String errorCode, long durationMs, LocalDateTime requestedAt,
        LocalDateTime completedAt, String encryptedPayload, boolean payloadTruncated) {
}
