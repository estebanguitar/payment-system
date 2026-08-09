package com.example.paymentsystem.application.service.outbox;

import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.domain.entity.outbox.PaymentOutbox;
import com.example.paymentsystem.infrastructure.outbox.OutboxProperties;
import com.example.paymentsystem.infrastructure.repository.outbox.PaymentOutboxRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 아웃박스 복구 실패 횟수를 독립 트랜잭션으로 기록한다. */
@Service
@RequiredArgsConstructor
public class OutboxStatusService {
    private final PaymentOutboxRepository outboxRepository;
    private final OutboxProperties properties;

    /** 재시도 횟수를 증가시키고 한도에 도달하면 최종 실패로 전환한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long outboxId) {
        PaymentOutbox outbox = outboxRepository.findByIdWithLock(outboxId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR));
        LocalDateTime now = LocalDateTime.now();
        outbox.recordFailure(now);
        if (outbox.getRetryCount() >= properties.maxRetryCount()) {
            outbox.markFailed(now);
        }
    }
}
