package com.example.paymentsystem.application.dto.audit;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 운영 감사 로그의 선택 검색 조건과 1-based 페이지 값을 전달한다. */
@Getter
@Builder
public class AuditLogSearchCondition {
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final String clientId;
    private final String requestPath;
    private final Integer responseStatus;
    private final String traceId;
    private final int page;
    private final int size;
}
