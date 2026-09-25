package com.ebrahimmorkas.wallet.ledger;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AmountRequest(
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description) {

    /** Canonical form used to detect an idempotency key being reused for a different request. */
    public String fingerprint(TransactionType type, UUID walletId) {
        return String.join("|", type.name(), walletId.toString(), amount.setScale(2).toPlainString(),
                String.valueOf(description));
    }
}
