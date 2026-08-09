package com.example.paymentsystem.dto.reconciliation;

import com.example.paymentsystem.domain.reconciliation.ReconciliationBreak;
import com.example.paymentsystem.domain.reconciliation.ReconciliationBreakType;
import java.time.LocalDateTime;

/** 운영 조회에 필요한 대사 불일치 정보만 반환한다. */
public record ReconciliationBreakResponse(Long id, ReconciliationBreakType breakType, Long paymentId, Long walletId,
        String expectedValue, String actualValue, String description, LocalDateTime detectedAt) {
    public static ReconciliationBreakResponse from(ReconciliationBreak source) {
        return new ReconciliationBreakResponse(source.getId(), source.getBreakType(), source.getPaymentId(),
                source.getWalletId(), source.getExpectedValue(), source.getActualValue(), source.getDescription(),
                source.getDetectedAt());
    }
}
