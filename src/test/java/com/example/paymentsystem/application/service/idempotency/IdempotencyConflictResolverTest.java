package com.example.paymentsystem.application.service.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.domain.entity.idempotency.IdempotencyRecord;
import com.example.paymentsystem.domain.entity.idempotency.IdempotencyRequestType;
import com.example.paymentsystem.infrastructure.repository.idempotency.IdempotencyRecordRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 글로벌 멱등 충돌 판정의 동일 요청·충돌·유실 분기를 검증한다. */
class IdempotencyConflictResolverTest {
    private final IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
    private final IdempotencyConflictResolver resolver = new IdempotencyConflictResolver(repository);

    /** resolve가 동일 요청의 연결 리소스를 반환하는지 확인한다. */
    @Test
    void resolveMatchingRequest() {
        IdempotencyRecord record = mock(IdempotencyRecord.class);
        when(record.matches(IdempotencyRequestType.PAYMENT, "fp")).thenReturn(true);
        when(record.getResourceId()).thenReturn(3L);
        when(repository.findById("K")).thenReturn(Optional.of(record));
        assertThat(resolver.resolve("K", IdempotencyRequestType.PAYMENT, "fp")).isEqualTo(3L);
    }

    /** resolve가 다른 요청과 레코드 유실을 각각 표준 오류로 변환하는지 확인한다. */
    @Test
    void rejectConflictAndMissingRecord() {
        IdempotencyRecord record = mock(IdempotencyRecord.class);
        when(repository.findById("K")).thenReturn(Optional.of(record));
        assertThatThrownBy(() -> resolver.resolve("K", IdempotencyRequestType.PAYMENT, "fp"))
                .extracting("errorCode").isEqualTo(ApplicationErrorCode.IDEMPOTENCY_CONFLICT);
        assertThatThrownBy(() -> resolver.resolve("NONE", IdempotencyRequestType.PAYMENT, "fp"))
                .extracting("errorCode").isEqualTo(ApplicationErrorCode.SYSTEM_ERROR);
    }
}
