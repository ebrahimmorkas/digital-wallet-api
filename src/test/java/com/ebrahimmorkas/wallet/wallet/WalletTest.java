package com.ebrahimmorkas.wallet.wallet;

import com.ebrahimmorkas.wallet.common.exception.BusinessRuleException;
import com.ebrahimmorkas.wallet.user.Role;
import com.ebrahimmorkas.wallet.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    private final Wallet wallet = Wallet.openFor(new User("a@b.com", "h", "A", Role.USER), "USD");

    @Test
    void creditAndDebitAdjustBalance() {
        wallet.credit(new BigDecimal("100.00"));
        wallet.debit(new BigDecimal("30.50"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("69.50");
    }

    @Test
    void userWalletCannotGoNegative() {
        wallet.credit(new BigDecimal("10.00"));

        assertThatThrownBy(() -> wallet.debit(new BigDecimal("10.01")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(wallet.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void frozenWalletRejectsMovements() {
        wallet.freeze();

        assertThatThrownBy(() -> wallet.credit(BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("WALLET_FROZEN");
    }
}
