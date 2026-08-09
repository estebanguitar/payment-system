package com.example.paymentsystem.reconciliation.domain;

import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.MismatchType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.Severity;

/** 검사기가 반환하는 비식별화된 불일치 근거다. */
public record ReconciliationFinding(
    String caseKey,
    MismatchType type,
    Severity severity,
    String customerId,
    Long walletId,
    Long paymentId,
    Long cancelId,
    Long outboxId,
    String expectedValue,
    String actualValue,
    String evidence) {}
