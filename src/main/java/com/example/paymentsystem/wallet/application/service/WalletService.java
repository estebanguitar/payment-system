package com.example.paymentsystem.wallet.application.service;

import com.example.paymentsystem.wallet.application.dto.WalletCommand;
import com.example.paymentsystem.wallet.application.dto.WalletResult;

import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.idempotency.application.service.IdempotencyConflictResolver;
import com.example.paymentsystem.idempotency.application.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.wallet.domain.Wallet;
import com.example.paymentsystem.customer.infrastructure.repository.CustomerRepository;
import com.example.paymentsystem.idempotency.domain.IdempotencyRequestType;
import com.example.paymentsystem.wallet.infrastructure.repository.WalletRepository;
import com.example.paymentsystem.wallet.infrastructure.repository.WalletTransactionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 지갑 생성·조회와 충전 멱등 재요청을 조율한다. */
@Service
@RequiredArgsConstructor
public class WalletService {
    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionService transactionService;
    private final WalletTransactionRepository transactionRepository;
    private final IdempotencyConflictResolver conflictResolver;
    private final IdempotencyFingerprintGenerator fingerprintGenerator;

    /** 고객당 하나의 잔액 0원 지갑을 생성한다. */
    @Transactional
    public WalletResult.Balance createWallet(String customerId) {
        if (!customerRepository.existsByCustomerId(customerId)) {
            throw new ApplicationException(ApplicationErrorCode.CUSTOMER_NOT_FOUND);
        }
        if (walletRepository.findByCustomerId(customerId).isPresent()) {
            throw new ApplicationException(ApplicationErrorCode.DUPLICATE_WALLET);
        }
        try {
            return WalletResult.Balance.from(walletRepository.saveAndFlush(Wallet.create(customerId, LocalDateTime.now())));
        } catch (DataIntegrityViolationException exception) {
            throw new ApplicationException(ApplicationErrorCode.DUPLICATE_WALLET, null, exception);
        }
    }

    /** 고객의 현재 지갑 잔액을 조회한다. */
    @Transactional(readOnly = true)
    public WalletResult.Balance getWallet(String customerId) {
        return WalletResult.Balance.from(walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.WALLET_NOT_FOUND)));
    }

    /** 충전을 수행하거나 동일 멱등 재요청의 현재 지갑 결과를 반환한다. */
    public WalletResult.TopUp topUp(WalletCommand.TopUp command) {
        try {
            return transactionService.topUp(command);
        } catch (DataIntegrityViolationException exception) {
            String fingerprint = fingerprintGenerator.topUp(command.getCustomerId(), command.getAmount());
            Long transactionId = conflictResolver.resolve(
                    command.getIdempotencyKey(), IdempotencyRequestType.WALLET_TOP_UP, fingerprint);
            return WalletResult.TopUp.from(command.getCustomerId(), transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.SYSTEM_ERROR)));
        }
    }
}
