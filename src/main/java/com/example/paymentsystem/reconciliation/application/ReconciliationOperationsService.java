package com.example.paymentsystem.reconciliation.application;

import com.example.paymentsystem.reconciliation.domain.ReconciliationActionHistory;
import com.example.paymentsystem.reconciliation.domain.ReconciliationCase;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.ActionType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.CaseStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationActionHistoryRepository;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationCaseRepository;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationRunRepository;
import com.example.paymentsystem.shared.application.dto.PageResult;
import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 대사 실행·Case 조회와 수동 판정 이력을 제공한다. */
@Service
@RequiredArgsConstructor
public class ReconciliationOperationsService {
  private final ReconciliationRunRepository runs;
  private final ReconciliationCaseRepository cases;
  private final ReconciliationActionHistoryRepository histories;
  private final List<ReconciliationDetector> detectors;

  /** 실행 이력을 최신순 조회한다. */
  public PageResult<ReconciliationRun> runs(int page, int size) {
    validatePage(page, size);
    return PageResult.from(
        runs.findAll(PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "startedAt"))));
  }

  /** 실행 단건을 조회한다. */
  public ReconciliationRun run(Long id) {
    return runs.findById(id)
        .orElseThrow(
            () -> new ApplicationException(ApplicationErrorCode.RECONCILIATION_RUN_NOT_FOUND, id));
  }

  /** Case를 최신 탐지순 조회한다. */
  public PageResult<ReconciliationCase> cases(int page, int size) {
    validatePage(page, size);
    return PageResult.from(
        cases.findAll(
            PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "lastDetectedAt"))));
  }

  /** Case와 조치 이력을 조회한다. */
  public CaseDetail detail(Long id) {
    ReconciliationCase c = findCase(id);
    return new CaseDetail(c, histories.findByCaseIdOrderByCreatedAtAsc(id));
  }

  /** 원장을 수정하지 않고 허용된 운영 상태 전이만 이력과 함께 기록한다. */
  @Transactional
  public ReconciliationCase action(
      Long id, ActionType action, String operator, String reason, String ref) {
    ReconciliationCase c = findCase(id);
    CaseStatus from = c.apply(action);
    histories.save(
        ReconciliationActionHistory.create(
            id, action, from, c.getStatus(), operator, reason, ref, LocalDateTime.now()));
    return c;
  }

  /** 동일 유형·대상의 불일치가 사라진 경우에만 Case를 해결 처리한다. */
  @Transactional
  public ReconciliationCase recheck(Long id, String operator, String reason) {
    ReconciliationCase c = findCase(id);
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

  private ReconciliationCase findCase(Long id) {
    return cases
        .findById(id)
        .orElseThrow(
            () -> new ApplicationException(ApplicationErrorCode.RECONCILIATION_CASE_NOT_FOUND, id));
  }

  private static void validatePage(int page, int size) {
    if (page < 1 || size < 1 || size > 100) {
      throw new ApplicationException(ApplicationErrorCode.INVALID_PAGE_REQUEST);
    }
  }
}
