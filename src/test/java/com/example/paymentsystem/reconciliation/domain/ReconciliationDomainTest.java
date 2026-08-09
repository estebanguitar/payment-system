package com.example.paymentsystem.reconciliation.domain;

import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.ActionType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.CaseStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.MismatchType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.RunStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.Severity;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.TriggerType;
import com.example.paymentsystem.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** 대사 실행과 Case 상태 불변식을 검증한다. */
class ReconciliationDomainTest {
  private final LocalDateTime now = LocalDateTime.of(2026, 8, 9, 10, 0);

  @Test
  void detectorFailureCompletesRunPartially() {
    ReconciliationRun run =
        ReconciliationRun.start("key", now.minusHours(1), now, TriggerType.MANUAL, "ops", now);
    run.complete(10, 2, 1, now);
    Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.PARTIALLY_FAILED);
  }

  @Test
  void resolvedCannotBeSelectedByManualAction() {
    ReconciliationFinding f =
        new ReconciliationFinding(
            "k",
            MismatchType.PAYMENT_DEBIT_MISSING,
            Severity.CRITICAL,
            "c",
            null,
            1L,
            null,
            null,
            "1",
            "0",
            "{}");
    ReconciliationCase c = ReconciliationCase.open(f, 1L, now);
    Assertions.assertThatThrownBy(() -> c.apply(ActionType.CONFIRM_RESOLVED))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void redetectedResolvedCaseRequiresActionAgain() {
    ReconciliationFinding f =
        new ReconciliationFinding(
            "k",
            MismatchType.PAYMENT_DEBIT_MISSING,
            Severity.CRITICAL,
            "c",
            null,
            1L,
            null,
            null,
            "1",
            "0",
            "{}");
    ReconciliationCase c = ReconciliationCase.open(f, 1L, now);
    c.resolve(now);
    c.redetect(f, 2L, now.plusHours(1));
    Assertions.assertThat(c.getStatus()).isEqualTo(CaseStatus.ACTION_REQUIRED);
  }
}
