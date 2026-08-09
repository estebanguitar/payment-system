package com.example.paymentsystem.audit.presentation.dto;

import com.example.paymentsystem.audit.application.dto.AuditLogSearchCondition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/** 운영 감사 로그 검색 조건과 페이지 범위를 검증한다. */
@Getter
@Setter
public class AuditLogSearchRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    private String clientId;
    private String requestPath;
    @Min(100) @Max(599)
    private Integer responseStatus;
    private String traceId;
    @Min(1)
    private int page = 1;
    @Min(1) @Max(100)
    private int size = 20;

    /** 검증된 HTTP 쿼리를 애플리케이션 검색 조건으로 변환한다. */
    public AuditLogSearchCondition toCondition() {
        return AuditLogSearchCondition.builder().startDate(startDate).endDate(endDate).clientId(clientId)
                .requestPath(requestPath).responseStatus(responseStatus).traceId(traceId).page(page).size(size).build();
    }
}
