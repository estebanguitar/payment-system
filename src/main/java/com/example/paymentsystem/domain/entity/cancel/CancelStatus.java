package com.example.paymentsystem.domain.entity.cancel;

/** 개별 결제 취소 시도의 처리 상태를 정의한다. */
public enum CancelStatus {
    PENDING,
    COMPLETED,
    FAILED
}
