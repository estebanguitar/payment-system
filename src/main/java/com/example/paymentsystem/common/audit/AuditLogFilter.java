package com.example.paymentsystem.common.audit;

import com.example.paymentsystem.common.web.TraceIdFilter;
import com.example.paymentsystem.service.audit.AuditLogService;
import com.example.paymentsystem.dto.audit.AuditLogCommand;
import com.example.paymentsystem.dto.audit.AuditPayload;
import com.example.paymentsystem.integration.pg.security.PayloadEncryptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.servlet.HandlerMapping;

/** API 요청·응답을 제한된 크기로 수집하고 민감 원문을 암호화해 감사 이력으로 저장한다. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuditLogFilter extends OncePerRequestFilter {
    private static final int MAX_BODY_CHARS = 4096;
    private static final String ENCRYPTED_CLIENT_MARKER = "encrypted";
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\\\"(?:password|token|authorization|cookie|cvv|cvc)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");

    private final AuditLogService auditLogService;
    private final AuditValueSanitizer sanitizer;
    private final ObjectMapper objectMapper;
    private final PayloadEncryptor encryptor;
    private final Counter failureCounter;

    /** 감사 저장과 JSON 직렬화·암호화 의존성을 구성한다. */
    public AuditLogFilter(AuditLogService auditLogService, AuditValueSanitizer sanitizer, ObjectMapper objectMapper,
            PayloadEncryptor encryptor, MeterRegistry registry) {
        this.auditLogService = auditLogService;
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
        this.encryptor = encryptor;
        this.failureCounter = Counter.builder("audit.log.write.failures").description("감사 로그 저장 실패 횟수")
                .register(registry);
    }

    /** API 처리 종료 후 응답을 복사하고 암호화된 요청 이력을 정확히 한 번 저장한다. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, MAX_BODY_CHARS);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        LocalDateTime requestedAt = LocalDateTime.now();
        long startedNanos = System.nanoTime();
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            LocalDateTime completedAt = LocalDateTime.now();
            try {
                record(cachedRequest, cachedResponse, requestedAt, completedAt, startedNanos);
            } catch (RuntimeException exception) {
                failureCounter.increment();
                log.error("감사 로그 저장에 실패했습니다. traceId={}",
                        request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE), exception);
            } finally {
                cachedResponse.copyBodyToResponse();
            }
        }
    }

    private void record(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
            LocalDateTime requestedAt, LocalDateTime completedAt, long startedNanos) {
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        String path = sanitizer.requestPath(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        String errorCode = (String) request.getAttribute(AuditRequestContext.ERROR_CODE_ATTRIBUTE);
        String requestBody = readable(request.getContentType()) ? body(request.getContentAsByteArray()) : null;
        String responseBody = readable(response.getContentType()) ? body(response.getContentAsByteArray()) : null;
        boolean truncated = request.getContentLengthLong() > MAX_BODY_CHARS
                || response.getContentAsByteArray().length > MAX_BODY_CHARS;
        AuditPayload payload = new AuditPayload(sanitizer.clientId(request.getHeader("X-Client-Id")),
                request.getRequestURI(), request.getQueryString(), request.getContentType(), limit(requestBody),
                response.getContentType(), limit(responseBody));
        String encryptedPayload = encryptor.encrypt(toJson(payload));
        long durationMs = Math.max(0, Duration.ofNanos(System.nanoTime() - startedNanos).toMillis());
        auditLogService.record(new AuditLogCommand(traceId, ENCRYPTED_CLIENT_MARKER, request.getMethod(), path,
                response.getStatus(), errorCode, durationMs, requestedAt, completedAt, encryptedPayload, truncated));
    }

    private String toJson(AuditPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("감사 payload 직렬화에 실패했습니다.", exception);
        }
    }

    private static String body(byte[] content) {
        return redact(new String(content, StandardCharsets.UTF_8));
    }

    private static String redact(String value) {
        return JSON_SECRET.matcher(value).replaceAll("$1***$2");
    }

    private static String limit(String value) {
        return value != null && value.length() > MAX_BODY_CHARS ? value.substring(0, MAX_BODY_CHARS) : value;
    }

    private static boolean readable(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("text/") || normalized.contains("json") || normalized.contains("xml")
                || normalized.contains("form-urlencoded");
    }

    /** `/api/` 외 모든 경로는 감사 수집에서 제외한다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
