package com.example.paymentsystem.reconciliation.application;

import com.example.paymentsystem.reconciliation.domain.ReconciliationFinding;
import java.time.LocalDateTime;
import java.util.List;

/** 업무 원장을 읽기 전용으로 검사하는 확장 지점이다. */
public interface ReconciliationDetector {
  /** 운영 지표에서 검사기를 구분할 수 있는 고유 이름을 반환한다. */
  String name();

  /** 지정한 시간 범위를 조회하되 원장을 변경하지 않고 불일치 목록만 반환한다. */
  List<ReconciliationFinding> detect(LocalDateTime from, LocalDateTime to);
}
