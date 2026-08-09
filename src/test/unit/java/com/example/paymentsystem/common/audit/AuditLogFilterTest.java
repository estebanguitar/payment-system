package com.example.paymentsystem.common.audit;

import com.example.paymentsystem.common.web.TraceIdFilter;
import com.example.paymentsystem.service.audit.AuditLogService;
import com.example.paymentsystem.dto.audit.AuditLogCommand;
import com.example.paymentsystem.integration.pg.security.AesGcmPayloadEncryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

/** 감사 Filter의 암호화 저장과 응답 보존을 검증한다. */
class AuditLogFilterTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    /** API 요청·응답 원문을 암호화하고 클라이언트 응답 body를 보존하는지 검증한다. */
    @Test
    void filterEncryptsPayloadAndRecordsNormalizedRequestOnce() throws Exception {
        AuditLogService service = Mockito.mock(AuditLogService.class);
        AesGcmPayloadEncryptor encryptor = new AesGcmPayloadEncryptor(KEY);
        AuditLogFilter filter = filter(service, encryptor);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/wallets/secret-customer");
        request.addHeader("X-Client-Id", "client-1");
        request.setContentType("application/json");
        request.setContent("{\"password\":\"secret\",\"amount\":1000}".getBytes(StandardCharsets.UTF_8));
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "trace-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                    "/api/v1/wallets/{customerId}");
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"result\":\"ok\"}");
            ((jakarta.servlet.http.HttpServletResponse) servletResponse).setStatus(200);
        };

        filter.doFilter(request, response, chain);

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        Mockito.verify(service).record(captor.capture());
        AuditLogCommand command = captor.getValue();
        Assertions.assertThat(command.requestPath()).isEqualTo("/api/v1/wallets/{customerId}");
        Assertions.assertThat(command.clientId()).isEqualTo("encrypted");
        Assertions.assertThat(command.encryptedPayload()).doesNotContain("secret-customer", "secret", "client-1");
        Assertions.assertThat(encryptor.decrypt(command.encryptedPayload()))
                .contains("client-1", "secret-customer", "***", "result").doesNotContain("\"secret\"");
        Assertions.assertThat(response.getContentAsString()).contains("result");
    }

    /** API 경로만 감사 대상으로 분류하고 문서·정적 경로는 제외하는지 검증한다. */
    @Test
    void filterTargetsOnlyApiPaths() {
        AuditLogFilter filter = filter(Mockito.mock(AuditLogService.class), new AesGcmPayloadEncryptor(KEY));

        Assertions.assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/v1/payments"))).isFalse();
        Assertions.assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/v3/api-docs"))).isTrue();
        Assertions.assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/swagger-ui.html"))).isTrue();
    }

    private static AuditLogFilter filter(AuditLogService service, AesGcmPayloadEncryptor encryptor) {
        return new AuditLogFilter(service, new AuditValueSanitizer(), new ObjectMapper(), encryptor,
                new SimpleMeterRegistry());
    }
}
