package com.example.paymentsystem.reconciliation.infrastructure.repository;

import com.example.paymentsystem.reconciliation.domain.ReconciliationActionHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Case별 운영 조치 이력을 시간순으로 제공한다. */
public interface ReconciliationActionHistoryRepository
    extends JpaRepository<ReconciliationActionHistory, Long> {
  List<ReconciliationActionHistory> findByCaseIdOrderByCreatedAtAsc(Long caseId);
}
