package com.ebrahimmorkas.wallet.transfer;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID sourceWalletId,
        @NotNull UUID destinationWalletId,
        @NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description) {
}
