package com.example.paymentsystem.infrastructure.persistence.wallet;

import com.example.paymentsystem.domain.wallet.Wallet;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/** 지갑 조회와 잔액 변경을 위한 비관적 잠금 조회를 제공한다. */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /** 잔액 변경 없이 고객의 지갑을 조회한다. */
    Optional<Wallet> findByCustomerId(String customerId);

    /** 동일 고객 지갑의 잔액 변경을 직렬화하도록 최대 3초간 쓰기 잠금을 획득한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select w from Wallet w where w.customerId = :customerId")
    Optional<Wallet> findByCustomerIdWithLock(@Param("customerId") String customerId);
}
