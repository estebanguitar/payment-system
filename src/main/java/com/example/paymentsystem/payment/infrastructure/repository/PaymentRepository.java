package com.example.paymentsystem.payment.infrastructure.repository;

import com.example.paymentsystem.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/** 결제 원장의 멱등·소유권 조회와 동시 취소용 비관적 잠금을 제공한다. */
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    /** 결제 멱등키로 기존 결제 원장을 조회한다. */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /** 고객 소유권이 일치하는 결제 원장을 조회한다. */
    Optional<Payment> findByIdAndCustomerId(Long id, String customerId);

    /** 동일 결제의 누적 취소액 갱신을 직렬화하도록 최대 3초간 쓰기 잠금을 획득한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select p from Payment p where p.id = :paymentId")
    Optional<Payment> findByIdWithLock(@Param("paymentId") Long paymentId);

    /** 고객의 결제 목록을 요청된 페이징과 정렬 조건으로 조회한다. */
    Page<Payment> findAllByCustomerId(String customerId, Pageable pageable);
}
