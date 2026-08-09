package com.example.paymentsystem.service.wallet;

import com.example.paymentsystem.dto.wallet.WalletCommand;
import com.example.paymentsystem.dto.wallet.WalletResult;

import com.example.paymentsystem.common.exception.ApplicationErrorCode;
import com.example.paymentsystem.common.exception.ApplicationException;
import com.example.paymentsystem.service.idempotency.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.domain.wallet.Wallet;
import com.example.paymentsystem.domain.wallet.WalletTransaction;
import com.example.paymentsystem.domain.idempotency.IdempotencyRecord;
import com.example.paymentsystem.repository.idempotency.IdempotencyRecordRepository;
import com.example.paymentsystem.domain.idempotency.IdempotencyRequestType;
import com.example.paymentsystem.repository.wallet.WalletRepository;
import com.example.paymentsystem.repository.wallet.WalletTransactionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 지갑 충전과 멱등 결과 연결을 하나의 트랜잭션으로 처리한다. */
@Service
@RequiredArgsConstructor
public class WalletTransactionService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final IdempotencyFingerprintGenerator fingerprintGenerator;

    /** 멱등키를 선점하고 잠긴 지갑에 충전 금액과 거래 이력을 반영한다. */
    @Transactional
    public WalletResult.TopUp topUp(WalletCommand.TopUp command) {
        LocalDateTime now = LocalDateTime.now();
        String fingerprint = fingerprintGenerator.topUp(command.getCustomerId(), command.getAmount());
        IdempotencyRecord record = IdempotencyRecord.reserve(command.getIdempotencyKey(),
                IdempotencyRequestType.WALLET_TOP_UP, fingerprint, now);
        idempotencyRepository.saveAndFlush(record);
        Wallet wallet = walletRepository.findByCustomerIdWithLock(command.getCustomerId())
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.WALLET_NOT_FOUND));
        long balance = wallet.increase(command.getAmount(), now);
        WalletTransaction transaction = transactionRepository.save(WalletTransaction.topUp(
                wallet.getId(), command.getAmount(), balance, command.getIdempotencyKey(), now));
        record.linkResource(transaction.getId(), now);
        return WalletResult.TopUp.from(command.getCustomerId(), transaction);
    }
}
