package com.example.paymentsystem.integration.pg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.integration.pg.pg.PgApprovalCommand;
import com.example.paymentsystem.integration.pg.pg.PgApprovalResult;
import com.example.paymentsystem.integration.pg.pg.PgCancelCommand;
import com.example.paymentsystem.integration.pg.pg.PgCancelResult;
import com.example.paymentsystem.integration.pg.pg.PgClientException;
import com.example.paymentsystem.integration.pg.pg.PgErrorType;
import com.example.paymentsystem.integration.pg.pg.PgResultStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 가상 PG의 승인·거절·오류·타임아웃과 결정적 멱등 응답을 검증한다. */
class FakePgClientTest {

    private FakePgClient pgClient;

    /** 각 테스트가 기본 승인 시나리오의 독립 PG Client를 사용하도록 준비한다. */
    @BeforeEach
    void setUp() {
        pgClient = new FakePgClient(new ObjectMapper(), new PgProperties(PgScenario.APPROVED));
    }

    /** 승인 응답에 거래 ID, 성공 코드 및 원문 JSON이 포함되는지 확인한다. */
    @Test
    void approvePayment() {
        PgApprovalResult response = pgClient.approve(approvalRequest());

        assertThat(response.getStatus()).isEqualTo(PgResultStatus.APPROVED);
        assertThat(response.getResponseCode()).isEqualTo("0000");
        assertThat(response.getRawPayload()).contains("transactionId", "10000");
    }

    /** 동일 멱등키의 반복 승인 요청이 같은 PG 거래 ID를 반환하는지 확인한다. */
    @Test
    void returnDeterministicTransactionId() {
        PgApprovalResult first = pgClient.approve(approvalRequest());
        PgApprovalResult second = pgClient.approve(approvalRequest());

        assertThat(second.getPgTransactionId()).isEqualTo(first.getPgTransactionId());
        assertThat(second.getRawPayload()).isEqualTo(first.getRawPayload());
        assertThat(first.getRawPayload()).isEqualTo(
                "{\"status\":\"APPROVED\",\"transactionId\":\""
                        + first.getPgTransactionId()
                        + "\",\"responseCode\":\"0000\",\"amount\":10000}");
    }

    /** 거절 시나리오가 예외가 아닌 명시적 거절 응답을 반환하는지 확인한다. */
    @Test
    void rejectPayment() {
        pgClient = new FakePgClient(new ObjectMapper(), new PgProperties(PgScenario.REJECTED));
        PgApprovalResult response = pgClient.approve(approvalRequest());

        assertThat(response.getStatus()).isEqualTo(PgResultStatus.REJECTED);
        assertThat(response.getResponseCode()).isEqualTo("REJECTED");
    }

    /** 오류와 타임아웃이 서로 다른 기술 오류 유형으로 전달되는지 확인한다. */
    @Test
    void classifyErrorAndTimeout() {
        FakePgClient errorClient = new FakePgClient(new ObjectMapper(), new PgProperties(PgScenario.ERROR));
        FakePgClient timeoutClient = new FakePgClient(new ObjectMapper(), new PgProperties(PgScenario.TIMEOUT));
        assertThatThrownBy(() -> errorClient.approve(approvalRequest()))
                .isInstanceOfSatisfying(PgClientException.class,
                        exception -> assertThat(exception.getErrorType()).isEqualTo(PgErrorType.ERROR));
        assertThatThrownBy(() -> timeoutClient.approve(approvalRequest()))
                .isInstanceOfSatisfying(PgClientException.class,
                        exception -> assertThat(exception.getErrorType()).isEqualTo(PgErrorType.TIMEOUT));
    }

    /** 결제 취소 승인 응답이 취소 거래 ID와 원문 JSON을 제공하는지 확인한다. */
    @Test
    void approveCancellation() {
        PgCancelResult response = pgClient.cancel(PgCancelCommand.builder()
                .paymentId(1L)
                .cancelId(2L)
                .idempotencyKey("CANCEL-IDEMPOTENCY-1")
                .amount(3_000)
                .build());

        assertThat(response.getStatus()).isEqualTo(PgResultStatus.APPROVED);
        assertThat(response.getPgCancelTransactionId()).startsWith("CANCEL-");
        assertThat(response.getRawPayload()).contains("cancelTransactionId");
    }

    /** 요청별 시나리오가 없으면 설정의 기본 시나리오를 사용하는지 확인한다. */
    @Test
    void useConfiguredDefaultScenario() {
        FakePgClient rejectedClient = new FakePgClient(
                new ObjectMapper(), new PgProperties(PgScenario.REJECTED));

        assertThat(rejectedClient.approve(approvalRequest()).getStatus())
                .isEqualTo(PgResultStatus.REJECTED);
    }

    private static PgApprovalCommand approvalRequest() {
        return PgApprovalCommand.builder()
                .paymentId(1L)
                .idempotencyKey("PAY-IDEMPOTENCY-1")
                .customerId("CUST-001")
                .amount(10_000)
                .build();
    }
}
