package com.example.paymentsystem.domain.pg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.common.exception.DomainErrorCode;
import com.example.paymentsystem.common.exception.DomainException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** PG 응답 로그가 암호화된 payload만 허용하는지 검증한다. */
class PgResponseLogTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    /** 암호화된 응답과 조회용 메타데이터를 함께 생성하는지 확인한다. */
    @Test
    void createEncryptedResponseLog() {
        PgResponseLog log = PgResponseLog.create(1L, "PG-1", "APPROVED", "encrypted", NOW);

        assertThat(log.getPaymentId()).isEqualTo(1L);
        assertThat(log.getEncryptedPayload()).isEqualTo("encrypted");
    }

    /** 빈 암호문은 PG 응답 로그로 생성할 수 없는지 확인한다. */
    @Test
    void rejectBlankEncryptedPayload() {
        assertThatThrownBy(() -> PgResponseLog.create(1L, "PG-1", "APPROVED", " ", NOW))
                .isInstanceOfSatisfying(DomainException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(DomainErrorCode.MISSING_ENCRYPTED_PAYLOAD);
                    assertThat(exception.getMessage()).isEqualTo(DomainErrorCode.MISSING_ENCRYPTED_PAYLOAD.getMessage());
                });
    }
}
