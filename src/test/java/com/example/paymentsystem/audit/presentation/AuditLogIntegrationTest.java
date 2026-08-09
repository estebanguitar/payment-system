package com.example.paymentsystem.audit.presentation;

import com.example.paymentsystem.audit.domain.AuditLog;
import com.example.paymentsystem.audit.infrastructure.repository.AuditLogRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/** 실제 HTTP 처리와 별도 트랜잭션 감사 저장의 연결을 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class AuditLogIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuditLogRepository repository;

    /** 미매핑 API 실패도 민감 경로 없이 감사 로그에 저장되는지 검증한다. */
    @Test
    void unmappedApiRequestIsAudited() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/private-customer-value")
                        .header("X-Client-Id", "integration-client").header("X-Trace-Id", "audit-test-trace"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        AuditLog log = repository.findAll().stream()
                .filter(item -> "audit-test-trace".equals(item.getTraceId())).findFirst().orElseThrow();
        Assertions.assertThat(log.getClientId()).isEqualTo("integration-client");
        Assertions.assertThat(log.getRequestPath()).isEqualTo("/unmapped");
        Assertions.assertThat(log.getResponseStatus()).isEqualTo(404);
        Assertions.assertThat(log.getErrorCode()).isEqualTo("API_RESOURCE_NOT_FOUND");
    }
}
