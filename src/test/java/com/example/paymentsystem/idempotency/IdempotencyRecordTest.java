package com.example.paymentsystem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.idempotency.domain.IdempotencyRecord;
import com.example.paymentsystem.idempotency.domain.IdempotencyRequestType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 글로벌 멱등키 레코드의 요청 비교와 결과 연결 불변식을 검증한다. */
class IdempotencyRecordTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);
    private static final String FINGERPRINT = "a".repeat(64);

    /** 선점한 요청의 유형과 SHA-256 지문이 모두 같을 때만 동일 요청으로 판단한다. */
    @Test
    void matchOriginalRequest() {
        IdempotencyRecord record = IdempotencyRecord.reserve(
                "GLOBAL-1", IdempotencyRequestType.PAYMENT, FINGERPRINT, NOW);

        assertThat(record.matches(IdempotencyRequestType.PAYMENT, FINGERPRINT)).isTrue();
        assertThat(record.matches(IdempotencyRequestType.WALLET_TOP_UP, FINGERPRINT)).isFalse();
    }

    /** 생성된 업무 리소스는 한 번만 연결할 수 있는지 확인한다. */
    @Test
    void linkResourceOnlyOnce() {
        IdempotencyRecord record = IdempotencyRecord.reserve(
                "GLOBAL-1", IdempotencyRequestType.PAYMENT, FINGERPRINT, NOW);

        record.linkResource(10L, NOW.plusSeconds(1));

        assertThat(record.getResourceId()).isEqualTo(10L);
        assertThatThrownBy(() -> record.linkResource(11L, NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    /** 정규화되지 않은 요청 지문은 저장 전에 거절하는지 확인한다. */
    @Test
    void rejectInvalidFingerprint() {
        assertThatThrownBy(() -> IdempotencyRecord.reserve(
                "GLOBAL-1", IdempotencyRequestType.PAYMENT, "invalid", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
