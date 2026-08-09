package com.example.paymentsystem.presentation.audit;

import com.example.paymentsystem.application.dto.audit.AuditLogCommand;
import com.example.paymentsystem.application.service.audit.AuditLogService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

/** 감사 Filter가 허용 메타데이터만 정확히 한 번 저장하는지 검증한다. */
class AuditLogFilterTest {
    /** API 응답 상태와 URI 템플릿을 감사 명령으로 기록하는지 검증한다. */
    @Test
    void filterRecordsNormalizedRequestOnce() throws Exception {
        AuditLogService service = Mockito.mock(AuditLogService.class);
        AuditLogFilter filter = new AuditLogFilter(service, new AuditValueSanitizer(), new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/secret-customer");
        request.addHeader("X-Client-Id", "client-1");
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "trace-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                    "/api/v1/wallets/{customerId}");
            ((MockHttpServletResponse) servletResponse).setStatus(404);
        };

        filter.doFilter(request, response, chain);

        ArgumentCaptor<AuditLogCommand> captor = ArgumentCaptor.forClass(AuditLogCommand.class);
        Mockito.verify(service).record(captor.capture());
        Assertions.assertThat(captor.getValue().requestPath()).isEqualTo("/api/v1/wallets/{customerId}");
        Assertions.assertThat(captor.getValue().responseStatus()).isEqualTo(404);
    }
}
