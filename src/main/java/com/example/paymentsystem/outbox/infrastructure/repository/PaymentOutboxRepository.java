package com.example.paymentsystem.outbox.infrastructure.repository;

import com.example.paymentsystem.outbox.domain.OutboxStatus;
import com.example.paymentsystem.outbox.domain.PaymentOutbox;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

/** 미발행 아웃박스 후보 조회와 개별 이벤트 잠금 조회를 제공한다. */
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    /** 지정 시각 이전에 고립된 상태별 이벤트를 오래된 순서와 제한 건수로 조회한다. */
    List<PaymentOutbox> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            OutboxStatus status, LocalDateTime createdBefore, Pageable pageable);

    /** 복구 중 중복 처리를 막기 위해 아웃박스 한 건에 쓰기 잠금을 획득한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PaymentOutbox o where o.id = :outboxId")
    Optional<PaymentOutbox> findByIdWithLock(@Param("outboxId") Long outboxId);

    /** 실행 가능하거나 임대가 만료된 이벤트 ID를 생성 시각 순서로 조회한다. */
    @Query(value = """
            select id from payment_outbox
             where (status = 'INIT' and next_attempt_at <= :now)
                or (status = 'PROCESSING' and lease_until < :now)
             order by created_at asc
            """, nativeQuery = true)
    List<Long> findClaimCandidates(@Param("now") LocalDateTime now, Pageable pageable);

    /** 현재 처리 가능한 이벤트 한 건을 조건부 갱신하여 Worker가 원자적으로 선점한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update payment_outbox
               set status = 'PROCESSING', worker_id = :workerId, lease_until = :leaseUntil, updated_at = :now
             where id = :outboxId
               and ((status = 'INIT' and next_attempt_at <= :now)
                 or (status = 'PROCESSING' and lease_until < :now))
            """, nativeQuery = true)
    int claim(@Param("outboxId") Long outboxId, @Param("workerId") String workerId,
              @Param("now") LocalDateTime now, @Param("leaseUntil") LocalDateTime leaseUntil);

    /** 운영 지표에 노출할 상태별 이벤트 수를 반환한다. */
    long countByStatus(OutboxStatus status);
}
