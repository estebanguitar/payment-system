package com.example.paymentsystem.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 전역 추적 ID 필터의 입력 검증과 전파 동작을 검증한다. */
class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter();

    /** 허용된 요청 추적 ID는 요청 속성과 응답 헤더에 그대로 전파한다. */
    @Test
    void reusesValidTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-1");
        FilterChain chain = (currentRequest, currentResponse) -> { };

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)).isEqualTo("trace-1");
        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("trace-1");
    }

    /** 허용되지 않은 요청 추적 ID는 외부 입력을 사용하지 않고 새 ID로 교체한다. */
    @Test
    void replacesInvalidTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "invalid trace id");
        FilterChain chain = (currentRequest, currentResponse) -> { };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER))
                .isNotBlank()
                .isNotEqualTo("invalid trace id");
    }
}
