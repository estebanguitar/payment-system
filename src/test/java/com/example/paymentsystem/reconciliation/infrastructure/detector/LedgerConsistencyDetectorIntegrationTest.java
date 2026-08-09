package com.example.paymentsystem.reconciliation.infrastructure.detector;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 범위 제한을 포함한 원장 대사 네이티브 SQL이 실제 스키마에서 실행되는지 검증한다. */
@SpringBootTest
@ActiveProfiles("test")
class LedgerConsistencyDetectorIntegrationTest {
  @Autowired private LedgerConsistencyDetector detector;

  /** 빈 원장에서도 모든 검사 SQL이 문법 오류 없이 실행되는지 확인한다. */
  @Test
  void executeAllLedgerQueries() {
    LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);

    assertThatCode(() -> detector.detect(from, from.plusDays(1))).doesNotThrowAnyException();
  }
}
