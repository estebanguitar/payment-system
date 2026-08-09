package com.example.paymentsystem.reconciliation.application;

import com.example.paymentsystem.reconciliation.domain.ReconciliationCase;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.TriggerType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationFinding;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationCaseRepository;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationRunRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 대사 검사기를 부분 실패 격리하여 실행하고 Case를 멱등 반영한다. */
@Service
public class ReconciliationService implements ReconciliationExecutionPort {
  private final List<ReconciliationDetector> detectors;
  private final ReconciliationRunRepository runs;
  private final ReconciliationCaseRepository cases;
  private final MeterRegistry metrics;
  private final Clock clock;

  @Autowired
  public ReconciliationService(
      List<ReconciliationDetector> detectors,
      ReconciliationRunRepository runs,
      ReconciliationCaseRepository cases,
      MeterRegistry metrics) {
    this(detectors, runs, cases, metrics, Clock.systemUTC());
  }

  ReconciliationService(
      List<ReconciliationDetector> d,
      ReconciliationRunRepository r,
      ReconciliationCaseRepository c,
      MeterRegistry m,
      Clock clock) {
    detectors = d;
    runs = r;
    cases = c;
    metrics = m;
    this.clock = clock;
  }

  /** 동일 범위 요청은 기존 Run을 반환하고 검사기 실패는 나머지 검사와 격리한다. */
  @Transactional
  public ReconciliationRun execute(LocalDateTime from, LocalDateTime to, String requester) {
    String key =
        UUID.nameUUIDFromBytes((from + "|" + to).getBytes(StandardCharsets.UTF_8)).toString();
    Optional<ReconciliationRun> existing = runs.findByRunKey(key);
    if (existing.isPresent()) return existing.get();
    LocalDateTime now = LocalDateTime.now(clock);
    ReconciliationRun run =
        runs.save(ReconciliationRun.start(key, from, to, TriggerType.MANUAL, requester, now));
    long checked = 0, mismatch = 0, failed = 0;
    for (ReconciliationDetector detector : detectors) {
      try {
        List<ReconciliationFinding> findings = detector.detect(from, to);
        checked += findings.size();
        mismatch += findings.size();
        for (ReconciliationFinding finding : findings) {
          ReconciliationCase item =
              cases
                  .findByCaseKey(finding.caseKey())
                  .orElseGet(() -> ReconciliationCase.open(finding, run.getId(), now));
          if (item.getId() != null) item.redetect(finding, run.getId(), now);
          cases.save(item);
          metrics
              .counter(
                  "reconciliation.mismatch.count",
                  "type",
                  finding.type().name(),
                  "severity",
                  finding.severity().name())
              .increment();
        }
      } catch (RuntimeException exception) {
        failed++;
        metrics
            .counter("reconciliation.detector.failures", "detector", detector.name())
            .increment();
      }
    }
    run.complete(checked, mismatch, failed, LocalDateTime.now(clock));
    metrics
        .counter("reconciliation.run.count", "status", run.getStatus().name(), "trigger", "MANUAL")
        .increment();
    return run;
  }
}
