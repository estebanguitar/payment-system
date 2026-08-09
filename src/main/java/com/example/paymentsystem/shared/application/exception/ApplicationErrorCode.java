package com.example.paymentsystem.shared.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Application 유스케이스에서 판정하는 표준 업무 오류 코드와 메시지를 정의한다. */
@Getter
@RequiredArgsConstructor
public enum ApplicationErrorCode {
  CUSTOMER_NOT_FOUND("CUSTOMER_NOT_FOUND", "고객을 찾을 수 없습니다."),
  WALLET_NOT_FOUND("WALLET_NOT_FOUND", "지갑을 찾을 수 없습니다."),
  DUPLICATE_WALLET("DUPLICATE_WALLET", "이미 고객 지갑이 존재합니다."),
  PAYMENT_NOT_FOUND("PAYMENT_NOT_FOUND", "결제를 찾을 수 없습니다."),
  CANCEL_NOT_FOUND("CANCEL_NOT_FOUND", "결제 취소를 찾을 수 없습니다."),
  AUDIT_LOG_NOT_FOUND("AUDIT_LOG_NOT_FOUND", "감사 로그를 찾을 수 없습니다."),
  RECONCILIATION_RUN_NOT_FOUND("RECONCILIATION_RUN_NOT_FOUND", "대사 실행을 찾을 수 없습니다."),
  RECONCILIATION_CASE_NOT_FOUND("RECONCILIATION_CASE_NOT_FOUND", "대사 Case를 찾을 수 없습니다."),
  API_RESOURCE_NOT_FOUND("API_RESOURCE_NOT_FOUND", "요청한 API 경로를 찾을 수 없습니다."),
  IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT", "멱등키가 다른 요청에 이미 사용되었습니다."),
  INVALID_PAGE_REQUEST("INVALID_PAGE_REQUEST", "페이지 요청 범위가 올바르지 않습니다."),
  INVALID_DATE_RANGE("INVALID_DATE_RANGE", "조회 시작일은 종료일보다 늦을 수 없습니다."),
  INVALID_AUDIT_SEARCH_CONDITION("INVALID_AUDIT_SEARCH_CONDITION", "감사 로그 검색 조건이 올바르지 않습니다."),
  SYSTEM_ERROR("SYSTEM_ERROR", "시스템 처리 중 오류가 발생했습니다.");

  private final String code;
  private final String message;
}
