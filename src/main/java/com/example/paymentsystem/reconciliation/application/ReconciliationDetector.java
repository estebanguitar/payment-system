package com.example.paymentsystem.reconciliation.application;
import com.example.paymentsystem.reconciliation.domain.ReconciliationFinding; import java.time.LocalDateTime; import java.util.List;
/** 업무 원장을 읽기 전용으로 검사하는 확장 지점이다. */
public interface ReconciliationDetector { String name(); List<ReconciliationFinding> detect(LocalDateTime from, LocalDateTime to); }
