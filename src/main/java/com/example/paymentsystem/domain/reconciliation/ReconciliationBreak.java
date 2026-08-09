package com.example.paymentsystem.domain.reconciliation;

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

/** 자동 보정 없이 탐지된 원장·PG·잔고 불일치를 추가 전용으로 기록한다. */
@Getter
@Entity
@Table(name = "reconciliation_break")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReconciliationBreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "break_key", nullable = false, unique = true, length = 200)
    private String breakKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "break_type", nullable = false, length = 64)
    private ReconciliationBreakType breakType;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "expected_value", nullable = false, length = 200)
    private String expectedValue;

    @Column(name = "actual_value", nullable = false, length = 200)
    private String actualValue;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    private ReconciliationBreak(String breakKey, ReconciliationBreakType breakType, Long paymentId, Long walletId,
            String expectedValue, String actualValue, String description, LocalDateTime detectedAt) {
        this.breakKey = breakKey;
        this.breakType = breakType;
        this.paymentId = paymentId;
        this.walletId = walletId;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.description = description;
        this.detectedAt = detectedAt;
    }

    /** 재현 가능한 키와 기대·실제 값을 사용해 불일치 기록을 생성한다. */
    public static ReconciliationBreak create(String breakKey, ReconciliationBreakType breakType, Long paymentId,
            Long walletId, String expectedValue, String actualValue, String description, LocalDateTime detectedAt) {
        return new ReconciliationBreak(breakKey, breakType, paymentId, walletId, expectedValue, actualValue,
                description, detectedAt);
    }
}
