package com.ebrahimmorkas.wallet.ledger;

import com.ebrahimmorkas.wallet.ledger.StatementService.StatementFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Statements")
@SecurityRequirement(name = "bearerAuth")
public class StatementController {

    private final StatementService statementService;

    @GetMapping("/api/wallets/{walletId}/transactions")
    @Operation(summary = "Paginated wallet statement, newest first, with optional filters")
    public Page<StatementEntryResponse> statement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID walletId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) EntryDirection direction,
            @Parameter(description = "Inclusive lower bound, ISO-8601 instant")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Exclusive upper bound, ISO-8601 instant")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return statementService.statement(UUID.fromString(jwt.getSubject()), walletId,
                new StatementFilter(type, direction, from, to), pageable);
    }
}
