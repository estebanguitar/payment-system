package com.example.paymentsystem.domain.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.paymentsystem.common.exception.InsufficientBalanceException;
import com.example.paymentsystem.common.exception.InvalidAmountException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 지갑의 잔액 변경 및 금액 불변식을 검증한다. */
class WalletTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    /** 신규 지갑은 잔액 0원으로 생성되는지 확인한다. */
    @Test
    void createWalletWithZeroBalance() {
        Wallet wallet = Wallet.create("CUST-100", NOW);

        assertThat(wallet.getBalance()).isZero();
        assertThat(wallet.getCreatedAt()).isEqualTo(NOW);
    }

    /** 충전과 차감 결과가 잔액과 반환값에 동일하게 반영되는지 확인한다. */
    @Test
    void increaseAndDecreaseBalance() {
        Wallet wallet = Wallet.create("CUST-100", NOW);

        assertThat(wallet.increase(10_000, NOW.plusSeconds(1))).isEqualTo(10_000);
        assertThat(wallet.decrease(3_000, NOW.plusSeconds(2))).isEqualTo(7_000);
        assertThat(wallet.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
    }

    /** 잔액보다 큰 차감은 잔액을 변경하지 않고 거절하는지 확인한다. */
    @Test
    void rejectInsufficientBalance() {
        Wallet wallet = Wallet.create("CUST-100", NOW);
        wallet.increase(1_000, NOW);

        assertThatThrownBy(() -> wallet.decrease(1_001, NOW))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(wallet.getBalance()).isEqualTo(1_000);
    }

    /** 0원·음수 및 long 범위를 넘는 잔액 변경을 거절하는지 확인한다. */
    @Test
    void rejectInvalidAmountAndOverflow() {
        Wallet wallet = Wallet.create("CUST-100", NOW);

        assertThatThrownBy(() -> wallet.increase(0, NOW)).isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> wallet.decrease(-1, NOW)).isInstanceOf(InvalidAmountException.class);
        wallet.increase(Long.MAX_VALUE, NOW);
        assertThatThrownBy(() -> wallet.increase(1, NOW)).isInstanceOf(InvalidAmountException.class);
    }
}
