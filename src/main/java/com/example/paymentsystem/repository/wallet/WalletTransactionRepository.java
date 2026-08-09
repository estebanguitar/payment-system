package com.example.paymentsystem.repository.wallet;

import com.example.paymentsystem.domain.wallet.WalletTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 지갑 잔액 변경 이력의 멱등 조회와 결제별 추적을 제공한다. */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /** 충전 멱등키로 기존 지갑 거래를 조회한다. */
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    /** 원 결제에 연결된 지갑 거래를 발생 순서대로 조회한다. */
    List<WalletTransaction> findAllByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
