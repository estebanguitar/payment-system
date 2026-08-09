package com.example.paymentsystem.application.port.out.pg;

/** PG 기술 실패의 원인을 호출 오류와 시간 초과로 구분한다. */
public enum PgErrorType {
    ERROR,
    TIMEOUT
}
