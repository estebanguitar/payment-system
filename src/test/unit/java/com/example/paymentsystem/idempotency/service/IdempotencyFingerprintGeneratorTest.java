package com.example.paymentsystem.service.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentsystem.domain.payment.CancelType;
import org.junit.jupiter.api.Test;

/** 멱등 지문의 결정성과 업무 유형 분리를 검증한다. */
class IdempotencyFingerprintGeneratorTest {
    private final IdempotencyFingerprintGenerator generator = new IdempotencyFingerprintGenerator();

    /** 같은 정규화 입력은 같은 SHA-256 지문을 만들고 업무 유형별 지문은 달라야 한다. */
    @Test
    void generateDeterministicAndSeparatedFingerprint() {
        assertThat(generator.payment(" CUST-1 ", 1000)).isEqualTo(generator.payment("CUST-1", 1000));
        assertThat(generator.payment("CUST-1", 1000)).hasSize(64)
                .isNotEqualTo(generator.topUp("CUST-1", 1000));
        assertThat(generator.cancel(1L, CancelType.FULL, 1000)).hasSize(64);
    }
}
