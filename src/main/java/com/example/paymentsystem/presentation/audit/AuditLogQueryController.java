package com.example.paymentsystem.presentation.audit;

import com.example.paymentsystem.application.service.audit.AuditLogService;
import com.example.paymentsystem.presentation.audit.dto.AuditLogResponse;
import com.example.paymentsystem.presentation.audit.dto.AuditLogSearchRequest;
import com.example.paymentsystem.presentation.common.ApiResponse;
import com.example.paymentsystem.presentation.query.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영자에게 감사 로그 조건 검색과 단건 조회를 제공한다. */
@RestController
@RequestMapping("/api/v1/ops/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log Query", description = "운영 감사 로그 조회 API")
public class AuditLogQueryController {
    private final AuditLogService auditLogService;

    /** 기간·클라이언트·경로·상태·추적 ID 조건으로 감사 로그를 검색한다. */
    @GetMapping
    @Operation(summary = "감사 로그 검색")
    public ApiResponse<PageResponse<AuditLogResponse>> search(@Valid @ModelAttribute AuditLogSearchRequest request) {
        return ApiResponse.success(PageResponse.from(auditLogService.search(request.toCondition()),
                AuditLogResponse::from));
    }

    /** 감사 로그 식별자로 안전한 메타데이터를 단건 조회한다. */
    @GetMapping("/{auditLogId}")
    @Operation(summary = "감사 로그 단건 조회")
    public ApiResponse<AuditLogResponse> get(@PathVariable Long auditLogId) {
        return ApiResponse.success(AuditLogResponse.from(auditLogService.get(auditLogId)));
    }
}
