package com.example.paymentsystem.integration.pg;

import com.example.paymentsystem.integration.pg.PgApprovalCommand;
import com.example.paymentsystem.integration.pg.PgApprovalResult;
import com.example.paymentsystem.integration.pg.PgCancelCommand;
import com.example.paymentsystem.integration.pg.PgCancelResult;
import com.example.paymentsystem.integration.pg.PgClient;
import com.example.paymentsystem.integration.pg.PgClientException;
import com.example.paymentsystem.integration.pg.PgErrorType;
import com.example.paymentsystem.integration.pg.PgResultStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 외부 서버 없이 PG 승인·거절·오류·타임아웃을 결정적으로 재현한다. */
@Component
@RequiredArgsConstructor
public class FakePgClient implements PgClient {

    private final ObjectMapper objectMapper;
    private final PgProperties properties;

    /** 요청 시나리오에 따라 결제 승인 응답 또는 분류된 기술 예외를 반환한다. */
    @Override
    public PgApprovalResult approve(PgApprovalCommand request) {
        validateApprovalRequest(request);
        PgScenario scenario = properties.defaultScenario();
        throwIfTechnicalFailure(scenario);

        PgResultStatus status = scenario == PgScenario.REJECTED
                ? PgResultStatus.REJECTED
                : PgResultStatus.APPROVED;
        String transactionId = deterministicId("PAY", request.getIdempotencyKey());
        String responseCode = status == PgResultStatus.APPROVED ? "0000" : "REJECTED";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status.name());
        payload.put("transactionId", transactionId);
        payload.put("responseCode", responseCode);
        payload.put("amount", request.getAmount());

        return PgApprovalResult.builder()
                .status(status)
                .pgTransactionId(transactionId)
                .responseCode(responseCode)
                .rawPayload(toJson(payload))
                .build();
    }

    /** 요청 시나리오에 따라 결제 취소 응답 또는 분류된 기술 예외를 반환한다. */
    @Override
    public PgCancelResult cancel(PgCancelCommand request) {
        validateCancelRequest(request);
        PgScenario scenario = properties.defaultScenario();
        throwIfTechnicalFailure(scenario);

        PgResultStatus status = scenario == PgScenario.REJECTED
                ? PgResultStatus.REJECTED
                : PgResultStatus.APPROVED;
        String transactionId = deterministicId("CANCEL", request.getIdempotencyKey());
        String responseCode = status == PgResultStatus.APPROVED ? "0000" : "REJECTED";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status.name());
        payload.put("cancelTransactionId", transactionId);
        payload.put("responseCode", responseCode);
        payload.put("amount", request.getAmount());

        return PgCancelResult.builder()
                .status(status)
                .pgCancelTransactionId(transactionId)
                .responseCode(responseCode)
                .rawPayload(toJson(payload))
                .build();
    }

    private static void throwIfTechnicalFailure(PgScenario scenario) {
        if (scenario == PgScenario.ERROR) {
            throw new PgClientException(PgErrorType.ERROR, "가상 PG 처리 중 오류가 발생했습니다.");
        }
        if (scenario == PgScenario.TIMEOUT) {
            throw new PgClientException(PgErrorType.TIMEOUT, "가상 PG 요청 시간이 초과되었습니다.");
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new PgClientException(PgErrorType.ERROR, "가상 PG 응답 생성에 실패했습니다.", exception);
        }
    }

    private static String deterministicId(String prefix, String idempotencyKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return prefix + "-" + HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private static void validateApprovalRequest(PgApprovalCommand request) {
        if (request == null || request.getPaymentId() == null || request.getPaymentId() <= 0
                || request.getCustomerId() == null || request.getCustomerId().isBlank()
                || request.getAmount() <= 0) {
            throw new IllegalArgumentException("유효한 결제 승인 요청이 필요합니다.");
        }
        requireIdempotencyKey(request.getIdempotencyKey());
    }

    private static void validateCancelRequest(PgCancelCommand request) {
        if (request == null || request.getPaymentId() == null || request.getPaymentId() <= 0
                || request.getCancelId() == null || request.getCancelId() <= 0
                || request.getAmount() <= 0) {
            throw new IllegalArgumentException("유효한 결제 취소 요청이 필요합니다.");
        }
        requireIdempotencyKey(request.getIdempotencyKey());
    }

    private static void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("PG 요청 멱등키는 필수입니다.");
        }
    }
}
