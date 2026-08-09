package com.example.paymentsystem.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** 실행 시 생성되는 OpenAPI 문서에 핵심 API 경로가 포함되는지 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {
    @Autowired
    private MockMvc mockMvc;

    /** 지갑·결제·고객·운영 API가 OpenAPI 계약에 게시되는지 검증한다. */
    @Test
    void openApiContainsPresentationEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/wallets']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/payments']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/customers/{customerId}/payments']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ops/payments']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ops/audit-logs']").exists());
    }
}
