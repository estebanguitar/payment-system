package com.example.paymentsystem.presentation.audit;

import com.example.paymentsystem.application.dto.audit.AuditLogCommand;
import com.example.paymentsystem.application.service.audit.AuditLogService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** API 요청 완료 결과를 민감 원문 없이 감사 로그로 저장한다. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuditLogFilter extends OncePerRequestFilter {
    private final AuditLogService auditLogService;
    private final AuditValueSanitizer sanitizer;
    private final Counter failureCounter;

    /** 감사 저장 서비스와 저장 실패 지표를 구성한다. */
    public AuditLogFilter(AuditLogService auditLogService, AuditValueSanitizer sanitizer, MeterRegistry registry) {
        this.auditLogService = auditLogService;
        this.sanitizer = sanitizer;
        this.failureCounter = Counter.builder("audit.log.write.failures").description("감사 로그 저장 실패 횟수")
                .register(registry);
    }

    /** API 처리 종료 시 최종 상태와 정규화 경로를 정확히 한 번 저장한다. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LocalDateTime requestedAt = LocalDateTime.now();
        long startedNanos = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            LocalDateTime completedAt = LocalDateTime.now();
            String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
            String clientId = sanitizer.clientId(request.getHeader("X-Client-Id"));
            String path = sanitizer.requestPath(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
            String errorCode = (String) request.getAttribute(AuditRequestContext.ERROR_CODE_ATTRIBUTE);
            long durationMs = Math.max(0, Duration.ofNanos(System.nanoTime() - startedNanos).toMillis());
            try {
                auditLogService.record(new AuditLogCommand(traceId, clientId, request.getMethod(), path,
                        response.getStatus(), errorCode, durationMs, requestedAt, completedAt));
            } catch (RuntimeException exception) {
                failureCounter.increment();
                log.error("감사 로그 저장에 실패했습니다. traceId={}", traceId, exception);
            }
        }
    }

    /** `/api/` 외 모든 경로는 감사 수집에서 제외한다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
