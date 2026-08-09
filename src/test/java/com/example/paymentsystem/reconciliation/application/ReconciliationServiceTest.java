package com.example.paymentsystem.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.reconciliation.domain.ReconciliationEnums.TriggerType;
import com.example.paymentsystem.reconciliation.domain.ReconciliationRun;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationCaseRepository;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationRunRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** 대사 실행 선점 경합과 실행 메타데이터 전달을 검증한다. */
class ReconciliationServiceTest {

  /** 동시 최초 실행의 고유 키 충돌 요청은 실패시키지 않고 선점된 실행을 반환한다. */
  @Test
  void returnsClaimedRunWhenConcurrentInsertLoses() {
    ReconciliationRunRepository runs = mock(ReconciliationRunRepository.class);
    ReconciliationRunClaimService claimService = mock(ReconciliationRunClaimService.class);
    LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
    LocalDateTime to = from.plusDays(1);
    LocalDateTime now = LocalDateTime.of(2026, 8, 9, 10, 0);
    ReconciliationRun claimed =
        ReconciliationRun.start("claimed-key", from, to, TriggerType.MANUAL, "first", now);
    when(runs.findByRunKey(any()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(claimed));
    when(claimService.claim(any(), any(), any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException("duplicate run key"));
    ReconciliationService service =
        new ReconciliationService(
            List.of(),
            runs,
            mock(ReconciliationCaseRepository.class),
            new SimpleMeterRegistry(),
            Clock.fixed(Instant.parse("2026-08-09T10:00:00Z"), ZoneOffset.UTC),
            claimService);

    ReconciliationRun result = service.execute(from, to, TriggerType.MANUAL, "second");

    assertThat(result).isSameAs(claimed);
  }
}
