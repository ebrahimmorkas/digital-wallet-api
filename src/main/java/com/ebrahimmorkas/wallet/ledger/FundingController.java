package com.ebrahimmorkas.wallet.ledger;

import com.ebrahimmorkas.wallet.idempotency.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallets/{walletId}")
@RequiredArgsConstructor
@Tag(name = "Deposits & withdrawals")
@SecurityRequirement(name = "bearerAuth")
public class FundingController {

    private final FundingService fundingService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/deposits")
    @Operation(summary = "Top up a wallet (simulated card payment)")
    public ResponseEntity<TransactionResponse> deposit(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID walletId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return idempotencyService.execute(userId, idempotencyKey, request.fingerprint(TransactionType.DEPOSIT, walletId),
                        () -> fundingService.deposit(userId, walletId, request))
                .toResponse(HttpStatus.CREATED);
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Withdraw funds to an external account")
    public ResponseEntity<TransactionResponse> withdraw(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID walletId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return idempotencyService.execute(userId, idempotencyKey, request.fingerprint(TransactionType.WITHDRAWAL, walletId),
                        () -> fundingService.withdraw(userId, walletId, request))
                .toResponse(HttpStatus.CREATED);
    }
}
