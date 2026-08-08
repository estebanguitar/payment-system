package com.example.paymentsystem.domain.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.domain.exception.DomainException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Customer 생성 시 필수 입력값 검증 경로를 확인한다. */
class CustomerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    @Test
    void createWithValidInputs() {
        Customer customer = Customer.create("CUS-001", "홍길동", "hong@example.com", NOW);

        assertThat(customer.getCustomerId()).isEqualTo("CUS-001");
        assertThat(customer.getName()).isEqualTo("홍길동");
        assertThat(customer.getEmail()).isEqualTo("hong@example.com");
        assertThat(customer.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectBlankCustomerId() {
        assertThatThrownBy(() -> Customer.create("", "홍길동", null, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Customer.create("   ", "홍길동", null, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectNullCustomerId() {
        assertThatThrownBy(() -> Customer.create(null, "홍길동", null, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectBlankName() {
        assertThatThrownBy(() -> Customer.create("CUS-001", "", null, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Customer.create("CUS-001", "   ", null, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectNullName() {
        assertThatThrownBy(() -> Customer.create("CUS-001", null, null, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectNullNow() {
        assertThatThrownBy(() -> Customer.create("CUS-001", "홍길동", null, null))
                .isInstanceOf(DomainException.class);
    }
}
