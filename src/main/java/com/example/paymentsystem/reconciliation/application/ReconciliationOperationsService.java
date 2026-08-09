package com.example.paymentsystem.reconciliation.application;

import com.example.paymentsystem.reconciliation.domain.ReconciliationActionHistory;
import com.example.paymentsystem.reconciliation.domain.ReconciliationCase;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.ActionType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.CaseStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationActionHistoryRepository;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationCaseRepository;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationRunRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 대사 실행·Case 조회와 수동 판정 이력을 제공한다. */
@Service
public class ReconciliationOperationsService {
  private final ReconciliationRunRepository runs;
  private final ReconciliationCaseRepository cases;
  private final ReconciliationActionHistoryRepository histories;
  private final List<ReconciliationDetector> detectors;

  public ReconciliationOperationsService(
      ReconciliationRunRepository r,
      ReconciliationCaseRepository c,
      ReconciliationActionHistoryRepository h,
      List<ReconciliationDetector> d) {
    runs = r;
    cases = c;
    histories = h;
    detectors = d;
  }

  /** 실행 이력을 최신순 조회한다. */
  public Page<ReconciliationRun> runs(int page, int size) {
    return runs.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt")));
  }

  /** 실행 단건을 조회한다. */
  public ReconciliationRun run(Long id) {
    return runs.findById(id).orElseThrow(() -> new IllegalArgumentException("대사 실행을 찾을 수 없습니다."));
  }

  /** Case를 최신 탐지순 조회한다. */
  public Page<ReconciliationCase> cases(int page, int size) {
    return cases.findAll(
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastDetectedAt")));
  }

  /** Case와 조치 이력을 조회한다. */
  public CaseDetail detail(Long id) {
    ReconciliationCase c =
        cases.findById(id).orElseThrow(() -> new IllegalArgumentException("대사 Case를 찾을 수 없습니다."));
    return new CaseDetail(c, histories.findByCaseIdOrderByCreatedAtAsc(id));
  }

  /** 원장을 수정하지 않고 허용된 운영 상태 전이만 이력과 함께 기록한다. */
  @Transactional
  public ReconciliationCase action(
      Long id, ActionType action, String operator, String reason, String ref) {
    ReconciliationCase c = cases.findById(id).orElseThrow();
    CaseStatus from = c.apply(action);
    histories.save(
        ReconciliationActionHistory.create(
            id, action, from, c.getStatus(), operator, reason, ref, LocalDateTime.now()));
    return c;
  }

  /** 동일 유형·대상의 불일치가 사라진 경우에만 Case를 해결 처리한다. */
  @Transactional
  public ReconciliationCase recheck(Long id, String operator, String reason) {
    ReconciliationCase c = cases.findById(id).orElseThrow();
    LocalDateTime from = c.getFirstDetectedAt().minusMinutes(1);
    LocalDateTime to = LocalDateTime.now().plusMinutes(1);
    boolean remains =
        detectors.stream()
            .flatMap(d -> d.detect(from, to).stream())
            .anyMatch(f -> f.caseKey().equals(c.getCaseKey()));
    CaseStatus before = c.getStatus();
    if (remains) {
      c.apply(ActionType.RECHECK);
    } else {
      c.resolve(LocalDateTime.now());
    }
    histories.save(
        ReconciliationActionHistory.create(
            id,
            ActionType.RECHECK,
            before,
            c.getStatus(),
            operator,
            reason,
            null,
            LocalDateTime.now()));
    return c;
  }

  /** Case와 변경 불가능한 조치 이력을 함께 반환한다. */
  public record CaseDetail(
      ReconciliationCase reconciliationCase, List<ReconciliationActionHistory> actions) {}
}
