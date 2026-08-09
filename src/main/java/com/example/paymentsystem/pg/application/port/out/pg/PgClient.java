package com.example.paymentsystem.pg.application.port.out.pg;

/** 결제 승인과 취소를 외부 PG 구현으로 교체할 수 있게 분리한 출력 포트다. */
public interface PgClient {

    /** 결제 승인 요청을 전송하고 승인 또는 거절 결과를 반환한다. */
    PgApprovalResult approve(PgApprovalCommand command);

    /** 결제 취소 요청을 전송하고 승인 또는 거절 결과를 반환한다. */
    PgCancelResult cancel(PgCancelCommand command);
}
