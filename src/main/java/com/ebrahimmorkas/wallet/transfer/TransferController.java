package com.ebrahimmorkas.wallet.transfer;

import com.ebrahimmorkas.wallet.idempotency.IdempotencyService;
import com.ebrahimmorkas.wallet.ledger.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @Operation(summary = "Send money to another user's wallet (same currency)",
            description = "Retries with the same Idempotency-Key return the original transaction instead of paying twice")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Client-generated unique key, e.g. a UUID")
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return idempotencyService.execute(userId, idempotencyKey, request.fingerprint(),
                        () -> transferService.transfer(userId, request))
                .toResponse(HttpStatus.CREATED);
    }
}
