package com.example.paymentsystem.service.reconciliation;

import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.domain.payment.PaymentStatus;
import com.example.paymentsystem.domain.pg.PgOperationType;
import com.example.paymentsystem.domain.pg.PgResponseLog;
import com.example.paymentsystem.domain.reconciliation.ReconciliationBreak;
import com.example.paymentsystem.domain.reconciliation.ReconciliationBreakType;
import com.example.paymentsystem.domain.wallet.Wallet;
import com.example.paymentsystem.domain.wallet.WalletTransaction;
import com.example.paymentsystem.domain.wallet.WalletTransactionType;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.repository.pg.PgResponseLogRepository;
import com.example.paymentsystem.repository.reconciliation.ReconciliationBreakRepository;
import com.example.paymentsystem.repository.wallet.WalletRepository;
import com.example.paymentsystem.repository.wallet.WalletTransactionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 결제·PG·지갑의 핵심 금액과 상태만 비교하는 경량 대사를 수행한다. */
@Service
@RequiredArgsConstructor
public class ReconciliationService {
    private final PaymentRepository paymentRepository;
    private final PgResponseLogRepository pgLogRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final ReconciliationBreakRepository breakRepository;
    private final Clock clock;

    /** 전체 결제와 지갑을 읽어 불일치만 중복 없이 저장한다. */
    @Transactional
    public void reconcile() {
        paymentRepository.findAll().forEach(this::reconcilePayment);
        walletRepository.findAll().forEach(this::reconcileWallet);
    }

    /** 탐지된 Break를 최신순으로 반환한다. */
    @Transactional(readOnly = true)
    public List<ReconciliationBreak> findAll() {
        return breakRepository.findAllByOrderByDetectedAtDescIdDesc();
    }

    private void reconcilePayment(Payment payment) {
        if (isCompleted(payment.getStatus())) {
            long walletAmount = transactionRepository.findAllByPaymentIdOrderByCreatedAtAsc(payment.getId()).stream()
                    .filter(transaction -> transaction.getTransactionType() == WalletTransactionType.PAYMENT)
                    .mapToLong(WalletTransaction::getAmount).sum();
            if (walletAmount != payment.getAmount()) {
                record(ReconciliationBreakType.PAYMENT_WALLET_AMOUNT, payment.getId(), null,
                        String.valueOf(payment.getAmount()), String.valueOf(walletAmount),
                        "완료 결제 금액과 지갑 PAYMENT 거래 합계가 다릅니다.");
            }
        }

        List<PgResponseLog> logs = pgLogRepository.findAllByPaymentIdAndOperationTypeOrderByReceivedAtAsc(
                payment.getId(), PgOperationType.PAYMENT_APPROVAL);
        if (logs.isEmpty()) {
            if (isCompleted(payment.getStatus())) {
                record(ReconciliationBreakType.PAYMENT_PG_LOG_MISSING, payment.getId(), null,
                        "PG 승인 로그", "없음", "완료 결제의 PG 승인 로그가 없습니다.");
            }
            return;
        }
        String code = logs.get(logs.size() - 1).getPgResponseCode();
        boolean pgApproved = "0000".equals(code);
        boolean internallyCompleted = isCompleted(payment.getStatus());
        if (pgApproved != internallyCompleted) {
            record(ReconciliationBreakType.PAYMENT_PG_STATUS, payment.getId(), null,
                    pgApproved ? "완료 계열" : "FAILED", payment.getStatus().name(),
                    "PG 승인 결과와 내부 결제 상태가 일치하지 않습니다.");
        }
    }

    private void reconcileWallet(Wallet wallet) {
        long expected = 0L;
        for (WalletTransaction transaction : transactionRepository.findAllByWalletIdOrderByCreatedAtAsc(wallet.getId())) {
            expected += switch (transaction.getTransactionType()) {
                case TOP_UP, REFUND -> transaction.getAmount();
                case PAYMENT -> -transaction.getAmount();
            };
        }
        if (expected != wallet.getBalance()) {
            record(ReconciliationBreakType.WALLET_BALANCE, null, wallet.getId(), String.valueOf(expected),
                    String.valueOf(wallet.getBalance()), "지갑 입출금 합계와 현재 잔고가 다릅니다.");
        }
    }

    private void record(ReconciliationBreakType type, Long paymentId, Long walletId, String expected, String actual,
            String description) {
        String target = paymentId != null ? "P" + paymentId : "W" + walletId;
        String breakKey = type.name() + ":" + target + ":" + expected + ":" + actual;
        if (breakRepository.existsByBreakKey(breakKey)) {
            return;
        }
        try {
            breakRepository.save(ReconciliationBreak.create(breakKey, type, paymentId, walletId, expected, actual,
                    description, LocalDateTime.now(clock)));
        } catch (DataIntegrityViolationException ignored) {
            // 동시에 같은 불일치를 탐지한 경우 고유키가 중복 기록을 막는다.
        }
    }

    private static boolean isCompleted(PaymentStatus status) {
        return status == PaymentStatus.COMPLETED || status == PaymentStatus.PARTIALLY_CANCELED
                || status == PaymentStatus.CANCELED;
    }
}
