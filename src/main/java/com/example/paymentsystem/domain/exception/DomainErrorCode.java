package com.example.paymentsystem.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 도메인 전반에서 재사용하는 업무 오류 코드와 표준 메시지를 정의한다. */
@Getter
@RequiredArgsConstructor
public enum DomainErrorCode {
    INVALID_REQUIRED_VALUE("INVALID_REQUIRED_VALUE", "필수 입력값이 누락되었거나 올바르지 않습니다."),
    INVALID_AMOUNT("INVALID_AMOUNT", "금액은 0보다 커야 합니다."),
    AMOUNT_OVERFLOW("INVALID_AMOUNT", "금액이 허용 범위를 초과합니다."),
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", "지갑 잔액이 부족합니다."),
    INVALID_PAYMENT_STATE("INVALID_PAYMENT_STATE", "현재 상태에서는 요청한 처리를 수행할 수 없습니다."),
    INVALID_CANCEL_AMOUNT("INVALID_CANCEL_AMOUNT", "취소 금액이 취소 가능 범위를 벗어났습니다."),
    MISSING_FAILURE_REASON("INVALID_REQUIRED_VALUE", "실패 처리에는 실패 사유가 필요합니다."),
    MISSING_ENCRYPTED_PAYLOAD("INVALID_REQUIRED_VALUE", "암호화된 PG 응답 원문은 필수입니다."),
    OUTBOX_RETRY_OVERFLOW("OUTBOX_RETRY_OVERFLOW", "아웃박스 재시도 횟수가 허용 범위를 초과했습니다.");

    private final String code;
    private final String message;
}
