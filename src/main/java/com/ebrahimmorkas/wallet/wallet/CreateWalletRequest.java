package com.ebrahimmorkas.wallet.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateWalletRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO 4217 code, e.g. USD") String currency) {
}
