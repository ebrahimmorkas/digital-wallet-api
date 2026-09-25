package com.ebrahimmorkas.wallet.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Back-office operations. Guarded twice: by URL in SecurityConfig and by method security here. */
@RestController
@RequestMapping("/api/admin/wallets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminWalletController {

    private final AdminWalletService adminWalletService;

    @GetMapping("/{walletId}")
    @Operation(summary = "Inspect any user wallet")
    public AdminWalletResponse find(@PathVariable UUID walletId) {
        return adminWalletService.find(walletId);
    }

    @PostMapping("/{walletId}/freeze")
    @Operation(summary = "Freeze a wallet: it can no longer send or receive money")
    public AdminWalletResponse freeze(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID walletId) {
        return adminWalletService.freeze(UUID.fromString(jwt.getSubject()), walletId);
    }

    @PostMapping("/{walletId}/unfreeze")
    @Operation(summary = "Unfreeze a wallet")
    public AdminWalletResponse unfreeze(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID walletId) {
        return adminWalletService.unfreeze(UUID.fromString(jwt.getSubject()), walletId);
    }
}
