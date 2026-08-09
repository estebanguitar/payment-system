package com.example.paymentsystem.dto.audit;

/** 암호화 전에 조립되는 API 요청·응답 원문 묶음이다. */
public record AuditPayload(String clientId, String requestUrl, String queryString,
        String requestContentType, String requestBody, String responseContentType, String responseBody) {
}
