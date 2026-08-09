package com.example.paymentsystem.worker;

import com.example.paymentsystem.outbox.application.port.in.OutboxRecoveryUseCase;
import com.example.paymentsystem.outbox.infrastructure.repository.PaymentOutboxRepository;
import com.example.paymentsystem.outbox.infrastructure.scheduling.OutboxProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/** Worker 선점과 처리 흐름의 단위 동작을 검증한다. */
class OutboxWorkerSchedulerTest {

    /** 선점에 성공한 후보만 복구 UseCase로 전달하는지 확인한다. */
    @Test
    void processOnlyClaimedEvent() {
        PaymentOutboxRepository repository = Mockito.mock(PaymentOutboxRepository.class);
        OutboxClaimService claimService = Mockito.mock(OutboxClaimService.class);
        OutboxRecoveryUseCase recovery = Mockito.mock(OutboxRecoveryUseCase.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC);
        OutboxWorkerMetrics metrics = new OutboxWorkerMetrics(new SimpleMeterRegistry(), repository, clock);
        Mockito.when(repository.findClaimCandidates(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(List.of(1L, 2L));
        Mockito.when(claimService.claim(ArgumentMatchers.eq(1L), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);
        Mockito.when(claimService.claim(ArgumentMatchers.eq(2L), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(false);
        OutboxWorkerScheduler scheduler = new OutboxWorkerScheduler(repository, claimService, recovery,
                new OutboxProperties(true, 1000, 1000, 10, 5, 30_000), metrics, clock);

        scheduler.process();

        Mockito.verify(recovery).recover(1L);
        Mockito.verify(recovery, Mockito.never()).recover(2L);
        Assertions.assertThat(metrics.lastHeartbeat()).isEqualTo(clock.instant());
    }
}
