package com.example.paymentsystem.scheduler.reconciliation;

import com.example.paymentsystem.service.reconciliation.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 설정된 주기마다 경량 원장·잔고 대사를 실행한다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.reconciliation.enabled", havingValue = "true")
public class ReconciliationScheduler {
    private final ReconciliationService reconciliationService;

    /** 대사 실패가 Scheduler 스레드를 종료하지 않도록 오류를 기록한다. */
    @Scheduled(fixedDelayString = "${payment.reconciliation.fixed-delay:300000}")
    public void reconcile() {
        try {
            reconciliationService.reconcile();
        } catch (RuntimeException exception) {
            log.error("경량 원장·잔고 대사 실행에 실패했습니다.", exception);
        }
    }
}
