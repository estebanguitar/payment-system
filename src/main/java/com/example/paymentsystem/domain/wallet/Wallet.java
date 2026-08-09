package com.example.paymentsystem.domain.wallet;

import com.example.paymentsystem.common.exception.DomainErrorCode;
import com.example.paymentsystem.common.exception.DomainException;
import com.example.paymentsystem.common.exception.InsufficientBalanceException;
import com.example.paymentsystem.common.exception.InvalidAmountException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 고객의 현재 잔액을 관리하고 음수 잔액을 방지하는 지갑 Aggregate다. */
@Getter
@Entity
@Table(name = "wallet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true, length = 64)
    private String customerId;

    @Column(nullable = false)
    private long balance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Wallet(String customerId, LocalDateTime now) {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "고객 식별자는 필수입니다.");
        }
        requireNow(now);
        this.customerId = customerId;
        this.balance = 0L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 고객의 초기 잔액이 0원인 신규 지갑을 생성한다. */
    public static Wallet create(String customerId, LocalDateTime now) {
        return new Wallet(customerId, now);
    }

    /** 양수 금액을 더하고 변경 후 잔액을 반환한다. */
    public long increase(long amount, LocalDateTime now) {
        requirePositive(amount);
        requireNow(now);
        try {
            balance = Math.addExact(balance, amount);
        } catch (ArithmeticException exception) {
            throw new InvalidAmountException(DomainErrorCode.AMOUNT_OVERFLOW);
        }
        updatedAt = now;
        return balance;
    }

    /** 잔액이 충분할 때만 양수 금액을 차감하고 변경 후 잔액을 반환한다. */
    public long decrease(long amount, LocalDateTime now) {
        requirePositive(amount);
        requireNow(now);
        if (balance < amount) {
            throw new InsufficientBalanceException();
        }
        balance -= amount;
        updatedAt = now;
        return balance;
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException();
        }
    }

    private static void requireNow(LocalDateTime now) {
        if (now == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "현재 시각은 필수입니다.");
        }
    }
}
