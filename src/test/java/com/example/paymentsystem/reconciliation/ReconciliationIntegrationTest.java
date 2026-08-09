package com.example.paymentsystem.reconciliation;

import com.example.paymentsystem.reconciliation.application.ReconciliationDetector;
import com.example.paymentsystem.reconciliation.application.ReconciliationExecutionPort;
import com.example.paymentsystem.reconciliation.infrastructure.repository.ReconciliationRunRepository;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/** 대사 실행의 DB 멱등성과 Detector 부분 실패 격리를 통합 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class ReconciliationIntegrationTest {
  @Autowired ReconciliationExecutionPort service;
  @Autowired ReconciliationRunRepository runs;

  @MockBean(name = "ledgerConsistencyDetector")
  ReconciliationDetector detector;

  @Test
  void sameRangeReturnsOneRunAndFailureIsIsolated() {
    LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0), to = from.plusDays(1);
    Mockito.when(detector.name()).thenReturn("broken");
    Mockito.when(detector.detect(from, to)).thenThrow(new IllegalStateException("failure"));
    var first = service.execute(from, to, "ops");
    var second = service.execute(from, to, "other");
    Assertions.assertThat(second.getId()).isEqualTo(first.getId());
    Assertions.assertThat(first.getStatus().name()).isEqualTo("PARTIALLY_FAILED");
    Assertions.assertThat(runs.count()).isEqualTo(1);
  }
}
