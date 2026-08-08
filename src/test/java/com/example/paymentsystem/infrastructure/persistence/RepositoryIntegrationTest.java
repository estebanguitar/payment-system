package com.example.paymentsystem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentsystem.domain.customer.Customer;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.domain.pg.PgResponseLog;
import com.example.paymentsystem.domain.wallet.Wallet;
import com.example.paymentsystem.infrastructure.persistence.customer.CustomerRepository;
import com.example.paymentsystem.infrastructure.persistence.idempotency.IdempotencyRecord;
import com.example.paymentsystem.infrastructure.persistence.idempotency.IdempotencyRecordRepository;
import com.example.paymentsystem.infrastructure.persistence.idempotency.IdempotencyRequestType;
import com.example.paymentsystem.infrastructure.persistence.payment.PaymentRepository;
import com.example.paymentsystem.infrastructure.persistence.pg.PgResponseLogRepository;
import com.example.paymentsystem.infrastructure.persistence.wallet.WalletRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 도메인별 Repository의 조회, 글로벌 유일성 및 비관적 잠금을 실제 H2에서 검증한다. */
@SpringBootTest
class RepositoryIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PgResponseLogRepository pgResponseLogRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** 고객·지갑·결제·PG 로그를 도메인별 Repository로 저장하고 조회하는지 확인한다. */
    @Test
    void persistAndQueryByDomainRepositories() {
        String suffix = UUID.randomUUID().toString();
        Customer customer = customerRepository.saveAndFlush(
                Customer.create("CUST-" + suffix, "인프라 테스트", null, NOW));
        Wallet wallet = walletRepository.saveAndFlush(Wallet.create(customer.getCustomerId(), NOW));
        Payment payment = paymentRepository.saveAndFlush(
                Payment.createPending("PAY-" + suffix, customer.getCustomerId(), 10_000, NOW));
        pgResponseLogRepository.saveAndFlush(
                PgResponseLog.create(payment.getId(), "PG-" + suffix, "0000", "encrypted", NOW));

        assertThat(customerRepository.existsByCustomerId(customer.getCustomerId())).isTrue();
        assertThat(walletRepository.findByCustomerId(customer.getCustomerId()))
                .get().extracting(Wallet::getId).isEqualTo(wallet.getId());
        assertThat(paymentRepository.findByIdAndCustomerId(payment.getId(), customer.getCustomerId()))
                .get().extracting(Payment::getId).isEqualTo(payment.getId());
        assertThat(pgResponseLogRepository.findAllByPaymentIdOrderByReceivedAtAsc(payment.getId()))
                .hasSize(1);
    }

    /** 동일 글로벌 멱등키의 동시 INSERT 중 정확히 하나만 성공하는지 확인한다. */
    @Test
    void allowOnlyOneConcurrentGlobalIdempotencyReservation() throws Exception {
        String key = "GLOBAL-" + UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> futures = List.of(
                    executor.submit(() -> reserveIdempotency(key, start)),
                    executor.submit(() -> reserveIdempotency(key, start)));
            start.countDown();

            long successCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    successCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(idempotencyRecordRepository.findById(key)).isPresent();
        } finally {
            executor.shutdownNow();
        }
    }

    /** 동일 지갑의 두 번째 쓰기 잠금 조회가 첫 트랜잭션 종료 전까지 대기하는지 확인한다. */
    @Test
    void serializeWalletUpdatesWithPessimisticLock() throws Exception {
        String customerId = "LOCK-WALLET-" + UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            customerRepository.save(Customer.create(customerId, "락 테스트", null, NOW));
            walletRepository.save(Wallet.create(customerId, NOW));
        });

        assertSecondLockWaitsUntilRelease(() -> walletRepository
                .findByCustomerIdWithLock(customerId)
                .orElseThrow());
    }

    /** 동일 결제의 두 번째 쓰기 잠금 조회가 첫 트랜잭션 종료 전까지 대기하는지 확인한다. */
    @Test
    void serializePaymentCancellationWithPessimisticLock() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Long paymentId = new TransactionTemplate(transactionManager).execute(status -> {
            Customer customer = customerRepository.save(
                    Customer.create("LOCK-PAY-" + suffix, "결제 락 테스트", null, NOW));
            return paymentRepository.save(Payment.createPending(
                    "LOCK-IDEM-" + suffix, customer.getCustomerId(), 10_000, NOW)).getId();
        });

        assertSecondLockWaitsUntilRelease(() -> paymentRepository.findByIdWithLock(paymentId).orElseThrow());
    }

    private boolean reserveIdempotency(String key, CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    idempotencyRecordRepository.saveAndFlush(IdempotencyRecord.reserve(
                            key, IdempotencyRequestType.PAYMENT, "b".repeat(64), NOW)));
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private void assertSecondLockWaitsUntilRelease(Runnable lockOperation) throws Exception {
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        lockOperation.run();
                        firstLocked.countDown();
                        await(releaseFirst);
                    }));
            firstLocked.await();
            Future<?> second = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> lockOperation.run()));

            Thread.sleep(200);
            assertThat(second.isDone()).isFalse();
            releaseFirst.countDown();
            first.get();
            second.get();
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 테스트 대기가 중단되었습니다.", exception);
        }
    }
}
