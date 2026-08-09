package com.example.paymentsystem.reconciliation.application;

import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.TriggerType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationRunRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 대사 실행 원장을 독립 트랜잭션으로 선점하여 동시 최초 요청의 경합을 격리한다. */
@Service
@RequiredArgsConstructor
public class ReconciliationRunClaimService {
  private final ReconciliationRunRepository runs;

  /** 고유 실행 키를 즉시 DB에 반영하여 한 요청만 실행 소유권을 얻도록 한다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ReconciliationRun claim(
      String key,
      LocalDateTime from,
      LocalDateTime to,
      TriggerType triggerType,
      String requester,
      LocalDateTime now) {
    return runs.saveAndFlush(
        ReconciliationRun.start(key, from, to, triggerType, requester, now));
  }
}
