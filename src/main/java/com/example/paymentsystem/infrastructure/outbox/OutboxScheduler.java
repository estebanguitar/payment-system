package com.example.paymentsystem.infrastructure.outbox;

import com.example.paymentsystem.domain.entity.outbox.OutboxStatus;
import com.example.paymentsystem.domain.entity.outbox.PaymentOutbox;
import com.example.paymentsystem.infrastructure.repository.outbox.PaymentOutboxRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 일정 시간 고립된 INIT 아웃박스를 찾아 Application 복구 처리기로 전달한다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.outbox.enabled", havingValue = "true")
public class OutboxScheduler {

    private final PaymentOutboxRepository outboxRepository;
    private final OutboxRecoveryPort recoveryPort;
    private final OutboxProperties properties;
    private final Clock clock;

    /** 설정된 주기마다 고립 이벤트를 제한 건수만 조회하여 서로 독립적으로 복구 요청한다. */
    @Scheduled(fixedDelayString = "#{@outboxScheduleDelayProvider.fixedDelay()}")
    public void recoverOrphanedEvents() {
        LocalDateTime threshold = LocalDateTime.now(clock)
                .minus(java.time.Duration.ofMillis(properties.orphanThreshold()));
        List<Long> candidateIds = outboxRepository
                .findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        OutboxStatus.INIT, threshold, PageRequest.of(0, properties.batchSize()))
                .stream()
                .filter(outbox -> outbox.getRetryCount() < properties.maxRetryCount())
                .map(PaymentOutbox::getId)
                .toList();

        for (Long outboxId : candidateIds) {
            try {
                recoveryPort.recover(outboxId);
            } catch (RuntimeException exception) {
                log.warn("아웃박스 복구 요청에 실패했습니다. outboxId={}", outboxId, exception);
            }
        }
    }

}
