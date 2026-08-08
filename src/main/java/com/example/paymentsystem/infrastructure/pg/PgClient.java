package com.example.paymentsystem.infrastructure.pg;

/** 결제 승인과 취소를 외부 PG 구현으로 교체할 수 있게 분리한 연동 계약이다. */
public interface PgClient {

    /** 결제 승인 요청을 전송하고 승인 또는 거절 응답을 반환한다. */
    PgApprovalResponse approve(PgApprovalRequest request);

    /** 결제 취소 요청을 전송하고 승인 또는 거절 응답을 반환한다. */
    PgCancelResponse cancel(PgCancelRequest request);
}
