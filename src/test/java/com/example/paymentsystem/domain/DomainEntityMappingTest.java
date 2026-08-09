package com.example.paymentsystem.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentsystem.domain.entity.cancel.CancelType;
import com.example.paymentsystem.domain.entity.cancel.PaymentCancel;
import com.example.paymentsystem.customer.domain.Customer;
import com.example.paymentsystem.domain.entity.outbox.PaymentOutbox;
import com.example.paymentsystem.domain.entity.payment.Payment;
import com.example.paymentsystem.pg.domain.PgResponseLog;
import com.example.paymentsystem.wallet.domain.Wallet;
import com.example.paymentsystem.wallet.domain.WalletTransaction;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway 스키마와 전체 도메인 엔티티의 JPA 매핑 호환성을 검증한다. */
@DataJpaTest
class DomainEntityMappingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Aggregate와 지원 원장을 FK 순서대로 저장하고 다시 조회할 수 있는지 확인한다. */
    @Test
    void persistAndLoadAllDomainEntities() {
        Customer customer = entityManager.persist(
                Customer.create("CUST-MAPPING", "매핑 테스트", "mapping@example.com", NOW));
        Wallet wallet = entityManager.persist(Wallet.create(customer.getCustomerId(), NOW));
        wallet.increase(20_000, NOW);

        Payment payment = entityManager.persist(
                Payment.createPending("PAY-MAPPING", customer.getCustomerId(), 10_000, NOW));
        PaymentCancel cancel = entityManager.persist(PaymentCancel.createPending(
                payment.getId(), "CANCEL-MAPPING", CancelType.PARTIAL, 1_000, 10_000, NOW));
        WalletTransaction transaction = entityManager.persist(WalletTransaction.refund(
                wallet.getId(), 1_000, wallet.getBalance(), payment.getId(), cancel.getId(), NOW));
        PgResponseLog pgLog = entityManager.persist(
                PgResponseLog.create(payment.getId(), "PG-MAPPING", "APPROVED", "encrypted", NOW));
        PaymentOutbox outbox = entityManager.persist(
                PaymentOutbox.createPaymentRequested(payment.getId(), "PAY-MAPPING", "{}", NOW));

        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Customer.class, customer.getId())).isNotNull();
        assertThat(entityManager.find(Wallet.class, wallet.getId())).isNotNull();
        assertThat(entityManager.find(Payment.class, payment.getId())).isNotNull();
        assertThat(entityManager.find(PaymentCancel.class, cancel.getId())).isNotNull();
        assertThat(entityManager.find(WalletTransaction.class, transaction.getId())).isNotNull();
        assertThat(entityManager.find(PgResponseLog.class, pgLog.getId())).isNotNull();
        assertThat(entityManager.find(PaymentOutbox.class, outbox.getId())).isNotNull();
    }

    /** 인라인 DDL 주석이 H2 메타데이터에 실제 컬럼 설명으로 반영되는지 확인한다. */
    @Test
    void exposeInlineColumnCommentsInDatabaseMetadata() {
        String comment = jdbcTemplate.queryForObject("""
                SELECT REMARKS
                  FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_NAME = 'PAYMENT'
                   AND COLUMN_NAME = 'STATUS'
                """, String.class);

        assertThat(comment).contains("결제 상태");
    }
}
