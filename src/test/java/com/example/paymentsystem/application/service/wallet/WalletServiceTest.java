package com.example.paymentsystem.application.service.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.paymentsystem.application.dto.wallet.WalletCommand;
import com.example.paymentsystem.application.dto.wallet.WalletResult;
import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.application.service.idempotency.IdempotencyConflictResolver;
import com.example.paymentsystem.application.util.IdempotencyFingerprintGenerator;
import com.example.paymentsystem.domain.entity.wallet.Wallet;
import com.example.paymentsystem.domain.entity.wallet.WalletTransaction;
import com.example.paymentsystem.infrastructure.repository.customer.CustomerRepository;
import com.example.paymentsystem.infrastructure.repository.wallet.WalletRepository;
import com.example.paymentsystem.infrastructure.repository.wallet.WalletTransactionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/** 지갑 서비스의 공개 메서드별 정상·실패·멱등 재요청 분기를 검증한다. */
class WalletServiceTest {
    private CustomerRepository customerRepository;
    private WalletRepository walletRepository;
    private WalletTransactionService transactionService;
    private WalletTransactionRepository transactionRepository;
    private IdempotencyConflictResolver conflictResolver;
    private WalletService service;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        walletRepository = mock(WalletRepository.class);
        transactionService = mock(WalletTransactionService.class);
        transactionRepository = mock(WalletTransactionRepository.class);
        conflictResolver = mock(IdempotencyConflictResolver.class);
        service = new WalletService(customerRepository, walletRepository, transactionService,
                transactionRepository, conflictResolver, new IdempotencyFingerprintGenerator());
    }

    /** createWallet이 고객의 신규 지갑을 생성하는지 확인한다. */
    @Test
    void createWallet() {
        when(customerRepository.existsByCustomerId("C1")).thenReturn(true);
        when(walletRepository.findByCustomerId("C1")).thenReturn(Optional.empty());
        when(walletRepository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            ReflectionTestUtils.setField(wallet, "id", 1L);
            return wallet;
        });
        assertThat(service.createWallet("C1").getBalance()).isZero();
    }

    /** createWallet이 미등록 고객과 중복 지갑을 구분하는지 확인한다. */
    @Test
    void rejectInvalidWalletCreation() {
        assertThatThrownBy(() -> service.createWallet("NONE"))
                .extracting("errorCode").isEqualTo(ApplicationErrorCode.CUSTOMER_NOT_FOUND);
        when(customerRepository.existsByCustomerId("C1")).thenReturn(true);
        when(walletRepository.findByCustomerId("C1")).thenReturn(Optional.of(mock(Wallet.class)));
        assertThatThrownBy(() -> service.createWallet("C1"))
                .extracting("errorCode").isEqualTo(ApplicationErrorCode.DUPLICATE_WALLET);
    }

    /** getWallet이 현재 잔액을 반환하고 미존재 지갑을 오류 처리하는지 확인한다. */
    @Test
    void getWallet() {
        Wallet wallet = Wallet.create("C1", LocalDateTime.now());
        ReflectionTestUtils.setField(wallet, "id", 1L);
        when(walletRepository.findByCustomerId("C1")).thenReturn(Optional.of(wallet));
        assertThat(service.getWallet("C1").getCustomerId()).isEqualTo("C1");
        assertThatThrownBy(() -> service.getWallet("NONE")).isInstanceOf(ApplicationException.class);
    }

    /** topUp이 최초 결과와 DB 충돌 후 기존 멱등 결과를 각각 반환하는지 확인한다. */
    @Test
    void topUpAndResolveDuplicate() {
        WalletCommand.TopUp command = WalletCommand.TopUp.builder()
                .customerId("C1").amount(1000).idempotencyKey("K1").build();
        WalletResult.TopUp first = WalletResult.TopUp.builder().transactionId(1L).build();
        when(transactionService.topUp(command)).thenReturn(first);
        assertThat(service.topUp(command)).isSameAs(first);

        when(transactionService.topUp(command)).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(conflictResolver.resolve(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(2L);
        WalletTransaction transaction = mock(WalletTransaction.class);
        when(transaction.getId()).thenReturn(2L);
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(transaction));
        assertThat(service.topUp(command).getTransactionId()).isEqualTo(2L);
    }
}
