package com.ebrahimmorkas.wallet.transfer;

import com.ebrahimmorkas.wallet.ledger.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send money to another user's wallet (same currency)")
    public TransactionResponse transfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(UUID.fromString(jwt.getSubject()), request);
    }
}
