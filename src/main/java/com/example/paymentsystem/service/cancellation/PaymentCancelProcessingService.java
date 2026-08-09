package com.example.paymentsystem.service.cancellation;

import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.integration.pg.pg.PgCancelResult;
import com.example.paymentsystem.integration.pg.security.PayloadEncryptor;
import com.example.paymentsystem.domain.cancellation.PaymentCancel;
import com.example.paymentsystem.domain.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.domain.payment.PaymentFailureReason;
import com.example.paymentsystem.domain.pg.PgResponseLog;
import com.example.paymentsystem.domain.wallet.Wallet;
import com.example.paymentsystem.domain.wallet.WalletTransaction;
import com.example.paymentsystem.repository.cancellation.PaymentCancelRepository;
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

/** PG 취소 결과를 원 결제·지갑·취소·로그·아웃박스에 원자적으로 반영한다. */
@Service
@RequiredArgsConstructor
public class PaymentCancelProcessingService {
    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository cancelRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PgResponseLogRepository pgLogRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final ObjectProvider<PayloadEncryptor> encryptorProvider;

    /** 승인된 취소 금액을 지갑에 환불하고 원 결제의 누적 취소액을 갱신한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void approve(Long paymentId, Long cancelId, Long outboxId, PgCancelResult result) {
        LocalDateTime now = LocalDateTime.now();
        Payment payment = lockedPayment(paymentId);
        PaymentCancel cancel = cancel(cancelId);
        Wallet wallet = walletRepository.findByCustomerIdWithLock(payment.getCustomerId())
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.WALLET_NOT_FOUND));
        long balance = wallet.increase(cancel.getAmount(), now);
        transactionRepository.save(WalletTransaction.refund(
                wallet.getId(), cancel.getAmount(), balance, paymentId, cancelId, now));
        payment.applyCancellation(cancel.getAmount(), now);
        cancel.complete(result.getPgCancelTransactionId(), now);
        saveLog(paymentId, result, now);
        outbox(outboxId).markCompleted(now);
    }

    /** PG의 취소 거절을 취소 실패와 암호화 로그로 종료한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reject(Long paymentId, Long cancelId, Long outboxId, PgCancelResult result) {
        LocalDateTime now = LocalDateTime.now();
        cancel(cancelId).fail(PaymentFailureReason.EXTERNAL_PAYMENT_REJECTED, now);
        saveLog(paymentId, result, now);
        outbox(outboxId).markCompleted(now);
    }

    /** PG 기술 실패를 취소 시스템 오류로 종료하고 아웃박스를 완료 처리한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failSystem(Long cancelId, Long outboxId) {
        LocalDateTime now = LocalDateTime.now();
        cancel(cancelId).fail(PaymentFailureReason.SYSTEM_ERROR, now);
        outbox(outboxId).markCompleted(now);
    }

    private Payment lockedPayment(Long id) {
        return paymentRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.PAYMENT_NOT_FOUND));
    }

    private PaymentCancel cancel(Long id) {
        return cancelRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.CANCEL_NOT_FOUND));
    }

    private PaymentOutbox outbox(Long id) {
        return outboxRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR));
    }

    private void saveLog(Long paymentId, PgCancelResult result, LocalDateTime now) {
        PayloadEncryptor encryptor = encryptorProvider.getIfAvailable();
        if (encryptor == null) {
            throw new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR);
        }
        pgLogRepository.save(PgResponseLog.create(paymentId, result.getPgCancelTransactionId(),
                result.getResponseCode(), encryptor.encrypt(result.getRawPayload()), now));
    }
}
