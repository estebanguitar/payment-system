package com.example.paymentsystem.repository.reconciliation;

import com.example.paymentsystem.domain.reconciliation.ReconciliationBreak;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 대사 불일치의 중복 확인, 추가 저장과 최신순 조회를 제공한다. */
public interface ReconciliationBreakRepository extends JpaRepository<ReconciliationBreak, Long> {
    boolean existsByBreakKey(String breakKey);

    List<ReconciliationBreak> findAllByOrderByDetectedAtDescIdDesc();
}
