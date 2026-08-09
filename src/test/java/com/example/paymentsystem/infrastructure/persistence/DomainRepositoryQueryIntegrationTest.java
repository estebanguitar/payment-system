package com.example.paymentsystem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentsystem.domain.cancel.CancelType;
import com.example.paymentsystem.domain.cancel.PaymentCancel;
import com.example.paymentsystem.domain.customer.Customer;
import com.example.paymentsystem.domain.outbox.OutboxStatus;
import com.example.paymentsystem.domain.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.payment.Payment;
import com.example.paymentsystem.domain.wallet.Wallet;
import com.example.paymentsystem.domain.wallet.WalletTransaction;
import com.example.paymentsystem.infrastructure.persistence.cancel.PaymentCancelRepository;
import com.example.paymentsystem.infrastructure.persistence.customer.CustomerRepository;
import com.example.paymentsystem.infrastructure.persistence.outbox.PaymentOutboxRepository;
import com.example.paymentsystem.infrastructure.persistence.payment.PaymentRepository;
import com.example.paymentsystem.infrastructure.persistence.wallet.WalletRepository;
import com.example.paymentsystem.infrastructure.persistence.wallet.WalletTransactionRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

/** 지갑·취소·아웃박스 Repository의 도메인별 추적 쿼리를 실제 스키마에서 검증한다. */
@SpringBootTest
class DomainRepositoryQueryIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentCancelRepository paymentCancelRepository;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    /** 결제에 연결된 지갑 거래와 취소 이력을 생성 순서대로 조회하는지 확인한다. */
    @Test
    void queryWalletAndCancelHistoryByPayment() {
        Fixture fixture = createFixture();
        PaymentCancel cancel = paymentCancelRepository.saveAndFlush(PaymentCancel.createPending(
                fixture.payment().getId(), "CANCEL-" + fixture.suffix(), CancelType.PARTIAL,
                1_000, fixture.payment().getAmount(), NOW));
        walletTransactionRepository.saveAndFlush(WalletTransaction.payment(
                fixture.wallet().getId(), 10_000, 10_000, fixture.payment().getId(), NOW));
        walletTransactionRepository.saveAndFlush(WalletTransaction.refund(
                fixture.wallet().getId(), 1_000, 11_000,
                fixture.payment().getId(), cancel.getId(), NOW.plusSeconds(1)));

        assertThat(walletTransactionRepository
                .findAllByPaymentIdOrderByCreatedAtAsc(fixture.payment().getId()))
                .extracting(WalletTransaction::getId)
                .hasSize(2);
        assertThat(paymentCancelRepository
                .findAllByPaymentIdOrderByCreatedAtAsc(fixture.payment().getId()))
                .extracting(PaymentCancel::getId)
                .containsExactly(cancel.getId());
    }

    /** 기준 시각 이전 INIT 아웃박스만 오래된 순서와 제한 건수로 조회하는지 확인한다. */
    @Test
    void queryOrphanedOutboxCandidates() {
        Fixture first = createFixture();
        Fixture second = createFixture();
        PaymentOutbox old = outboxRepository.saveAndFlush(PaymentOutbox.createPaymentRequested(
                first.payment().getId(), "OUTBOX-" + first.suffix(), "{}", NOW.minusMinutes(2)));
        outboxRepository.saveAndFlush(PaymentOutbox.createPaymentRequested(
                second.payment().getId(), "OUTBOX-" + second.suffix(), "{}", NOW));

        assertThat(outboxRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                OutboxStatus.INIT, NOW.minusMinutes(1), PageRequest.of(0, 1)))
                .extracting(PaymentOutbox::getId)
                .containsExactly(old.getId());
    }

    private Fixture createFixture() {
        String suffix = UUID.randomUUID().toString();
        Customer customer = customerRepository.saveAndFlush(
                Customer.create("QUERY-" + suffix, "쿼리 테스트", null, NOW));
        Wallet wallet = walletRepository.saveAndFlush(Wallet.create(customer.getCustomerId(), NOW));
        Payment payment = paymentRepository.saveAndFlush(Payment.createPending(
                "PAY-QUERY-" + suffix, customer.getCustomerId(), 20_000, NOW));
        return new Fixture(suffix, wallet, payment);
    }

    /** 테스트 간 반복되는 지갑·결제 원장 식별자를 묶는다. */
    private record Fixture(String suffix, Wallet wallet, Payment payment) {
    }
}
