package com.example.paymentsystem.infrastructure.pg;

/** 가상 PG 호출 실패를 일반 오류와 타임아웃으로 구분한다. */
public enum PgErrorType {
    ERROR,
    TIMEOUT
}
