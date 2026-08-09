package com.example.paymentsystem.worker;

import com.example.paymentsystem.outbox.application.port.in.OutboxRecoveryUseCase;
import com.example.paymentsystem.outbox.domain.PaymentOutbox;
import com.example.paymentsystem.outbox.infrastructure.repository.PaymentOutboxRepository;
import com.example.paymentsystem.outbox.infrastructure.scheduling.OutboxProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 실행 가능 이벤트를 선점하여 PG 후속 처리를 수행하고 실패 이벤트를 재시도 또는 격리한다. */
@Slf4j
@Component
public class OutboxWorkerScheduler {

    private final PaymentOutboxRepository repository;
    private final OutboxClaimService claimService;
    private final OutboxRecoveryUseCase recoveryUseCase;
    private final OutboxProperties properties;
    private final OutboxWorkerMetrics metrics;
    private final Clock clock;
    private final String workerId = UUID.randomUUID().toString();

    /** Worker 처리에 필요한 의존성을 주입한다. */
    public OutboxWorkerScheduler(PaymentOutboxRepository repository, OutboxClaimService claimService,
                                 OutboxRecoveryUseCase recoveryUseCase, OutboxProperties properties,
                                 OutboxWorkerMetrics metrics, Clock clock) {
        this.repository = repository;
        this.claimService = claimService;
        this.recoveryUseCase = recoveryUseCase;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** 주기마다 후보를 조회하고 원자적으로 선점한 이벤트만 처리한다. */
    @Scheduled(fixedDelayString = "${payment.outbox.fixed-delay:60000}")
    public void process() {
        metrics.heartbeat();
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> ids = repository.findClaimCandidates(now, PageRequest.of(0, properties.batchSize()));
        for (Long id : ids) {
            if (!claimService.claim(id, workerId, now,
                    now.plus(Duration.ofMillis(properties.leaseDuration())))) {
                continue;
            }
            processClaimed(id);
        }
    }

    private void processClaimed(Long id) {
        try {
            recoveryUseCase.recover(id);
            metrics.success();
        } catch (RuntimeException exception) {
            metrics.failure();
            PaymentOutbox outbox = repository.findById(id).orElse(null);
            if (outbox != null && outbox.getRetryCount() >= properties.maxRetryCount()) {
                outbox.markDeadLetter(LocalDateTime.now(clock));
                repository.save(outbox);
            } else {
                metrics.retry();
            }
            log.warn("Outbox 처리에 실패했습니다. outboxId={}, workerId={}", id, workerId, exception);
        }
    }
}
