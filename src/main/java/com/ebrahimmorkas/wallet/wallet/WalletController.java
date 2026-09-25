package com.ebrahimmorkas.wallet.wallet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a wallet in a supported currency (USD, EUR, GBP, INR)")
    public WalletResponse open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateWalletRequest request) {
        return walletService.open(userId(jwt), request.currency());
    }

    @GetMapping
    @Operation(summary = "List my wallets")
    public List<WalletResponse> findMine(@AuthenticationPrincipal Jwt jwt) {
        return walletService.findMine(userId(jwt));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get one of my wallets")
    public WalletResponse findOne(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID walletId) {
        return walletService.findMine(userId(jwt), walletId);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
