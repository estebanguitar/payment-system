package com.example.paymentsystem.infrastructure.repository.outbox;

import com.example.paymentsystem.domain.entity.outbox.OutboxStatus;
import com.example.paymentsystem.domain.entity.outbox.PaymentOutbox;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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
}
