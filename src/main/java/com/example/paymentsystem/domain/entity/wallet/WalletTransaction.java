package com.example.paymentsystem.domain.entity.wallet;

import com.example.paymentsystem.shared.domain.exception.DomainErrorCode;
import com.example.paymentsystem.shared.domain.exception.DomainException;
import com.example.paymentsystem.shared.domain.exception.InvalidAmountException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 충전·결제·환불로 발생한 지갑 잔액 변경 이력을 보관한다. */
@Getter
@Entity
@Table(name = "wallet_transaction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private WalletTransactionType transactionType;

    @Column(nullable = false)
    private long amount;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "cancel_id")
    private Long cancelId;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private WalletTransaction(Long walletId, WalletTransactionType transactionType, long amount,
                              long balanceAfter, Long paymentId, Long cancelId,
                              String idempotencyKey, LocalDateTime now) {
        requireId(walletId, "지갑 식별자는 필수입니다.");
        requirePositive(amount);
        if (balanceAfter < 0) {
            throw new InvalidAmountException();
        }
        if (now == null) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, "현재 시각은 필수입니다.");
        }
        this.walletId = walletId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.paymentId = paymentId;
        this.cancelId = cancelId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = now;
    }

    /** 멱등키를 포함한 지갑 충전 거래 이력을 생성한다. */
    public static WalletTransaction topUp(Long walletId, long amount, long balanceAfter,
                                          String idempotencyKey, LocalDateTime now) {
        requireText(idempotencyKey, "충전 멱등키는 필수입니다.");
        return new WalletTransaction(walletId, WalletTransactionType.TOP_UP, amount,
                balanceAfter, null, null, idempotencyKey, now);
    }

    /** 원 결제를 참조하는 지갑 차감 거래 이력을 생성한다. */
    public static WalletTransaction payment(Long walletId, long amount, long balanceAfter,
                                            Long paymentId, LocalDateTime now) {
        requireId(paymentId, "결제 식별자는 필수입니다.");
        return new WalletTransaction(walletId, WalletTransactionType.PAYMENT, amount,
                balanceAfter, paymentId, null, null, now);
    }

    /** 원 결제와 취소 거래를 참조하는 지갑 환불 이력을 생성한다. */
    public static WalletTransaction refund(Long walletId, long amount, long balanceAfter,
                                           Long paymentId, Long cancelId, LocalDateTime now) {
        requireId(paymentId, "결제 식별자는 필수입니다.");
        requireId(cancelId, "취소 식별자는 필수입니다.");
        return new WalletTransaction(walletId, WalletTransactionType.REFUND, amount,
                balanceAfter, paymentId, cancelId, null, now);
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException();
        }
    }

    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, message);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(DomainErrorCode.INVALID_REQUIRED_VALUE, message);
        }
    }
}
