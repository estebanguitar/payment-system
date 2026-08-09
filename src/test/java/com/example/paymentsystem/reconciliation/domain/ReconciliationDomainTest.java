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

  /** 재탐지 시 심각도와 관련 식별자를 최신 탐지 결과로 갱신한다. */
  @Test
  void redetectRefreshesLatestFindingAttributes() {
    ReconciliationCase c =
        ReconciliationCase.open(
            new ReconciliationFinding(
                "k",
                MismatchType.PAYMENT_DEBIT_MISSING,
                Severity.MEDIUM,
                "old-customer",
                null,
                1L,
                null,
                null,
                "1",
                "0",
                "{}"),
            1L,
            now);
    ReconciliationFinding latest =
        new ReconciliationFinding(
            "k",
            MismatchType.PAYMENT_DEBIT_MISSING,
            Severity.CRITICAL,
            "new-customer",
            null,
            2L,
            null,
            null,
            "2",
            "0",
            "{\"latest\":true}");

    c.redetect(latest, 2L, now.plusMinutes(1));

    Assertions.assertThat(c.getSeverity()).isEqualTo(Severity.CRITICAL);
    Assertions.assertThat(c.getCustomerId()).isEqualTo("new-customer");
    Assertions.assertThat(c.getPaymentId()).isEqualTo(2L);
    Assertions.assertThat(c.getEvidence()).isEqualTo("{\"latest\":true}");
  }

  /** 필수 탐지 근거가 없으면 Case를 생성하지 않는다. */
  @Test
  void rejectCaseWithoutRequiredEvidence() {
    ReconciliationFinding invalid =
        new ReconciliationFinding(
            "k",
            MismatchType.PAYMENT_DEBIT_MISSING,
            Severity.CRITICAL,
            "customer",
            null,
            1L,
            null,
            null,
            "1",
            "0",
            " ");

    Assertions.assertThatThrownBy(() -> ReconciliationCase.open(invalid, 1L, now))
        .isInstanceOf(DomainException.class);
  }
}
