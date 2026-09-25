package com.ebrahimmorkas.wallet.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        String currency,
        UUID sourceWalletId,
        UUID destinationWalletId,
        String description,
        Instant createdAt) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount(),
                transaction.getCurrency(), transaction.getDebitWalletId(), transaction.getCreditWalletId(),
                transaction.getDescription(), transaction.getCreatedAt());
    }
}
