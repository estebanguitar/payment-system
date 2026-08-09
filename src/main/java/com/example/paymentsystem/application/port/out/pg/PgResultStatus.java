package com.example.paymentsystem.application.port.out.pg;

/** PG 호출이 승인되었는지 업무적으로 거절되었는지 나타낸다. */
public enum PgResultStatus {
    APPROVED,
    REJECTED
}
