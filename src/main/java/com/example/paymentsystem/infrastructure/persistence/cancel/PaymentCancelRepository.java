package com.example.paymentsystem.infrastructure.persistence.cancel;

import com.example.paymentsystem.domain.cancel.PaymentCancel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 결제 취소 시도의 멱등 조회와 원 결제별 이력 조회를 제공한다. */
public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {

    /** 취소 멱등키로 기존 취소 시도를 조회한다. */
    Optional<PaymentCancel> findByIdempotencyKey(String idempotencyKey);

    /** 원 결제의 모든 취소 시도를 생성 순서대로 조회한다. */
    List<PaymentCancel> findAllByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
