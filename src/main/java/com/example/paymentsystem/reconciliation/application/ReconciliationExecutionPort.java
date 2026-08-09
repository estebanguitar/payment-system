package com.example.paymentsystem.reconciliation.application;

import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.TriggerType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun;
import java.time.LocalDateTime;

/** API와 별도 Batch/Worker가 동일한 대사를 실행하기 위한 입력 Port다. */
public interface ReconciliationExecutionPort {
  /** 동일 범위의 중복 실행을 방지하며 대사를 수행한다. */
  ReconciliationRun execute(
      LocalDateTime from, LocalDateTime to, TriggerType triggerType, String requester);
}
