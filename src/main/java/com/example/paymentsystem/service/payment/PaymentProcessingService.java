package com.example.paymentsystem.service.payment;

import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.integration.pg.pg.PgApprovalResult;
import com.example.paymentsystem.integration.pg.security.PayloadEncryptor;
import com.example.paymentsystem.common.exception.InsufficientBalanceException;
import com.example.paymentsystem.domain.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.domain.payment.PaymentFailureReason;
import com.example.paymentsystem.domain.pg.PgResponseLog;
import com.example.paymentsystem.domain.wallet.Wallet;
import com.example.paymentsystem.domain.wallet.WalletTransaction;
import com.example.paymentsystem.repository.outbox.PaymentOutboxRepository;
import com.example.paymentsystem.repository.payment.PaymentRepository;
import com.example.paymentsystem.repository.pg.PgResponseLogRepository;
import com.example.paymentsystem.repository.wallet.WalletRepository;
import com.example.paymentsystem.repository.wallet.WalletTransactionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** PG 승인 결과를 결제·지갑·로그·아웃박스에 원자적으로 반영한다. */
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PgResponseLogRepository pgLogRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final ObjectProvider<PayloadEncryptor> encryptorProvider;

    /** 승인된 PG 결과를 반영하며 잔액 부족은 최종 결제 실패로 종료한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void approve(Long paymentId, Long outboxId, PgApprovalResult result) {
        Payment payment = pendingPayment(paymentId);
        PaymentOutbox outbox = initOutbox(outboxId);
        LocalDateTime now = LocalDateTime.now();
        try {
            Wallet wallet = walletRepository.findByCustomerIdWithLock(payment.getCustomerId())
                    .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.WALLET_NOT_FOUND));
            long balance = wallet.decrease(payment.getAmount(), now);
            transactionRepository.save(WalletTransaction.payment(
                    wallet.getId(), payment.getAmount(), balance, payment.getId(), now));
            payment.complete(now);
        } catch (InsufficientBalanceException exception) {
            payment.fail(PaymentFailureReason.INSUFFICIENT_BALANCE, now);
        }
        saveLog(paymentId, result.getPgTransactionId(), result.getResponseCode(), result.getRawPayload(), now);
        outbox.markCompleted(now);
    }

    /** PG 업무 거절을 외부 결제 거절 상태와 로그로 반영한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reject(Long paymentId, Long outboxId, PgApprovalResult result) {
        LocalDateTime now = LocalDateTime.now();
        pendingPayment(paymentId).fail(PaymentFailureReason.EXTERNAL_PAYMENT_REJECTED, now);
        saveLog(paymentId, result.getPgTransactionId(), result.getResponseCode(), result.getRawPayload(), now);
        initOutbox(outboxId).markCompleted(now);
    }

    /** PG 기술 실패를 시스템 오류로 종료하고 재시도 대상에서 제외한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failSystem(Long paymentId, Long outboxId) {
        LocalDateTime now = LocalDateTime.now();
        pendingPayment(paymentId).fail(PaymentFailureReason.SYSTEM_ERROR, now);
        initOutbox(outboxId).markCompleted(now);
    }

    private Payment pendingPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND));
    }

    private PaymentOutbox initOutbox(Long outboxId) {
        return outboxRepository.findByIdWithLock(outboxId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR));
    }

    private void saveLog(Long paymentId, String transactionId, String code, String rawPayload, LocalDateTime now) {
        PayloadEncryptor encryptor = encryptorProvider.getIfAvailable();
        if (encryptor == null) {
            throw new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR);
        }
        pgLogRepository.save(PgResponseLog.create(
                paymentId, transactionId, code, encryptor.encrypt(rawPayload), now));
    }
}
