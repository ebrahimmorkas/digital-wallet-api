package com.ebrahimmorkas.wallet.ledger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallets/{walletId}")
@RequiredArgsConstructor
@Tag(name = "Deposits & withdrawals")
@SecurityRequirement(name = "bearerAuth")
public class FundingController {

    private final FundingService fundingService;

    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Top up a wallet (simulated card payment)")
    public TransactionResponse deposit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID walletId,
                                       @Valid @RequestBody AmountRequest request) {
        return fundingService.deposit(UUID.fromString(jwt.getSubject()), walletId, request);
    }

    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Withdraw funds to an external account")
    public TransactionResponse withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID walletId,
                                        @Valid @RequestBody AmountRequest request) {
        return fundingService.withdraw(UUID.fromString(jwt.getSubject()), walletId, request);
    }
}
