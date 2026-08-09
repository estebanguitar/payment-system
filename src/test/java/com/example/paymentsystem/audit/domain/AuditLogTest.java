package com.example.paymentsystem.audit.domain;

import com.example.paymentsystem.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** 감사 로그 생성 불변식과 성공 상태 계산을 검증한다. */
class AuditLogTest {
    /** 정상 메타데이터로 추가 전용 감사 로그가 생성되는지 검증한다. */
    @Test
    void createBuildsAuditLog() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 9, 10, 0);
        AuditLog log = AuditLog.create("trace-1", "client-1", "GET", "/api/v1/wallets/{customerId}",
                200, null, 12, requestedAt, requestedAt.plusNanos(12_000_000));

        Assertions.assertThat(log.isSuccess()).isTrue();
        Assertions.assertThat(log.getRequestPath()).doesNotContain("customer-1");
    }

    /** 종료 시각이 시작보다 빠르면 생성을 거절하는지 검증한다. */
    @Test
    void createRejectsInvalidTimeRange() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 9, 10, 0);
        Assertions.assertThatThrownBy(() -> AuditLog.create("trace-1", "client-1", "GET", "/api/v1/test",
                        200, null, 0, requestedAt, requestedAt.minusSeconds(1)))
                .isInstanceOf(DomainException.class);
    }
}
