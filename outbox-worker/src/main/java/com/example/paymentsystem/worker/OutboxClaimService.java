package com.example.paymentsystem.worker;

import com.example.paymentsystem.outbox.infrastructure.repository.PaymentOutboxRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 조건부 DB 갱신으로 여러 Worker 사이의 단일 이벤트 선점을 보장한다. */
@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final PaymentOutboxRepository repository;

    /** 별도 짧은 트랜잭션에서 임대를 획득하고 성공 여부를 반환한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long outboxId, String workerId, LocalDateTime now, LocalDateTime leaseUntil) {
        return repository.claim(outboxId, workerId, now, leaseUntil) == 1;
    }
}
