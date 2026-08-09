package com.example.paymentsystem.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.reconciliation.domain.ReconciliationCase;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.CaseStatus;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.MismatchType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.Severity;
import com.example.paymentsystem.reconciliation.domain.ReconciliationFinding;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationActionHistoryRepository;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationCaseRepository;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationRunRepository;
import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 재검증 결과에 따른 Case 상태 전이와 조회 오류 규약을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ReconciliationOperationsServiceTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

  @Mock private ReconciliationRunRepository runs;
  @Mock private ReconciliationCaseRepository cases;
  @Mock private ReconciliationActionHistoryRepository histories;
  @Mock private ReconciliationDetector detector;

  private ReconciliationOperationsService service;

  @BeforeEach
  void setUp() {
    service = new ReconciliationOperationsService(runs, cases, histories, List.of(detector));
  }

  /** 불일치가 남아 있으면 해결하지 않고 추가 조치 필요 상태로 전환한다. */
  @Test
  void recheckKeepsCaseActionRequiredWhenMismatchRemains() {
    ReconciliationFinding finding = finding("case-1");
    ReconciliationCase reconciliationCase = ReconciliationCase.open(finding, 1L, NOW);
    when(cases.findById(1L)).thenReturn(Optional.of(reconciliationCase));
    when(detector.detect(any(), any())).thenReturn(List.of(finding));

    ReconciliationCase result = service.recheck(1L, "operator", "다시 확인");

    assertThat(result.getStatus()).isEqualTo(CaseStatus.ACTION_REQUIRED);
    verify(histories).save(any());
  }

  /** 불일치가 사라진 경우에만 Case를 해결 상태로 전환한다. */
  @Test
  void recheckResolvesCaseWhenMismatchDisappears() {
    ReconciliationCase reconciliationCase = ReconciliationCase.open(finding("case-1"), 1L, NOW);
    when(cases.findById(1L)).thenReturn(Optional.of(reconciliationCase));
    when(detector.detect(any(), any())).thenReturn(List.of());

    ReconciliationCase result = service.recheck(1L, "operator", "정상화 확인");

    assertThat(result.getStatus()).isEqualTo(CaseStatus.RESOLVED);
    assertThat(result.getResolvedAt()).isNotNull();
    verify(histories).save(any());
  }

  /** 존재하지 않는 Case는 표준 Application 오류 코드로 반환한다. */
  @Test
  void recheckRejectsMissingCaseWithNotFoundCode() {
    when(cases.findById(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.recheck(9L, "operator", "확인"))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ApplicationErrorCode.RECONCILIATION_CASE_NOT_FOUND));
  }

  private static ReconciliationFinding finding(String caseKey) {
    return new ReconciliationFinding(
        caseKey,
        MismatchType.PAYMENT_DEBIT_MISSING,
        Severity.CRITICAL,
        "customer-1",
        null,
        1L,
        null,
        null,
        "1000",
        "0",
        "{}");
  }
}
