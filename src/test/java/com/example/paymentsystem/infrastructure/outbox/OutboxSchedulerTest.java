package com.example.paymentsystem.infrastructure.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.domain.outbox.OutboxStatus;
import com.example.paymentsystem.domain.outbox.PaymentOutbox;
import com.example.paymentsystem.infrastructure.persistence.outbox.PaymentOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** 고립 아웃박스 조회와 이벤트별 오류 격리 동작을 검증한다. */
class OutboxSchedulerTest {

    /** 한 이벤트 복구 실패가 다음 후보의 복구 요청을 막지 않는지 확인한다. */
    @Test
    void continueRecoveryAfterIndividualFailure() {
        PaymentOutboxRepository repository = mock(PaymentOutboxRepository.class);
        OutboxRecoveryPort recoveryPort = mock(OutboxRecoveryPort.class);
        PaymentOutbox first = mock(PaymentOutbox.class);
        PaymentOutbox second = mock(PaymentOutbox.class);
        when(first.getId()).thenReturn(1L);
        when(first.getRetryCount()).thenReturn(1);
        when(second.getId()).thenReturn(2L);
        when(second.getRetryCount()).thenReturn(0);
        when(repository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                eq(OutboxStatus.INIT), any(), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        doThrow(new RuntimeException("recovery failure")).when(recoveryPort).recover(1L);
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        OutboxScheduler scheduler = new OutboxScheduler(
                repository, recoveryPort, new OutboxProperties(true, 60_000, 60_000, 50, 5), clock);

        scheduler.recoverOrphanedEvents();

        verify(recoveryPort).recover(1L);
        verify(recoveryPort).recover(2L);
    }

    /** 최대 재시도 횟수에 도달한 후보는 복구 처리기로 전달하지 않는지 확인한다. */
    @Test
    void skipCandidateAtRetryLimit() {
        PaymentOutboxRepository repository = mock(PaymentOutboxRepository.class);
        OutboxRecoveryPort recoveryPort = mock(OutboxRecoveryPort.class);
        PaymentOutbox exhausted = mock(PaymentOutbox.class);
        when(exhausted.getId()).thenReturn(1L);
        when(exhausted.getRetryCount()).thenReturn(5);
        when(repository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                eq(OutboxStatus.INIT), any(), any(Pageable.class)))
                .thenReturn(List.of(exhausted));
        OutboxScheduler scheduler = new OutboxScheduler(repository, recoveryPort,
                new OutboxProperties(true, 60_000, 60_000, 50, 5), Clock.systemUTC());

        scheduler.recoverOrphanedEvents();

        verify(recoveryPort, org.mockito.Mockito.never()).recover(any());
    }
}
