package com.ebrahimmorkas.wallet.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One line of a wallet statement, seen from that wallet's perspective. */
public record StatementEntryResponse(
        UUID transactionId,
        TransactionType type,
        EntryDirection direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        UUID counterpartyWalletId,
        String description,
        Instant createdAt) {

    static StatementEntryResponse from(LedgerEntry entry) {
        Transaction transaction = entry.getTransaction();
        UUID counterparty = entry.getDirection() == EntryDirection.DEBIT
                ? transaction.getCreditWalletId()
                : transaction.getDebitWalletId();
        return new StatementEntryResponse(transaction.getId(), transaction.getType(), entry.getDirection(),
                entry.getAmount(), entry.getBalanceAfter(), counterparty, transaction.getDescription(),
                entry.getCreatedAt());
    }
}
