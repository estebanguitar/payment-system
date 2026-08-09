package com.example.paymentsystem.worker;

import com.example.paymentsystem.outbox.domain.OutboxStatus;
import com.example.paymentsystem.outbox.infrastructure.repository.PaymentOutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** 처리 결과와 heartbeat 및 격리 건수를 Actuator/Micrometer 지표로 제공한다. */
@Component
public class OutboxWorkerMetrics {

    private final Counter success;
    private final Counter failure;
    private final Counter retry;
    private final AtomicReference<Instant> heartbeat = new AtomicReference<>(Instant.EPOCH);
    private final Clock clock;

    /** Worker 운영 지표와 상태별 gauge를 등록한다. */
    public OutboxWorkerMetrics(MeterRegistry registry, PaymentOutboxRepository repository, Clock clock) {
        this.clock = clock;
        success = registry.counter("payment.outbox.worker.success");
        failure = registry.counter("payment.outbox.worker.failure");
        retry = registry.counter("payment.outbox.worker.retry");
        registry.gauge("payment.outbox.worker.dead_letter", repository,
                value -> value.countByStatus(OutboxStatus.DEAD_LETTER));
        registry.gauge("payment.outbox.worker.pending", repository,
                value -> value.countByStatus(OutboxStatus.INIT));
        registry.gauge("payment.outbox.worker.heartbeat.epoch", heartbeat,
                value -> value.get().getEpochSecond());
    }

    /** Worker 생존 시각을 현재 시각으로 갱신한다. */
    public void heartbeat() { heartbeat.set(clock.instant()); }

    /** 성공 처리 건수를 증가시킨다. */
    public void success() { success.increment(); }

    /** 실패 처리 건수를 증가시킨다. */
    public void failure() { failure.increment(); }

    /** 재시도 예정 건수를 증가시킨다. */
    public void retry() { retry.increment(); }

    /** 마지막 heartbeat 시각을 반환한다. */
    public Instant lastHeartbeat() { return heartbeat.get(); }
}
