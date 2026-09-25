package com.ebrahimmorkas.wallet.ledger;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/** Composable, optional filters for wallet statements. A {@code null} argument means "no filter". */
final class LedgerEntrySpecifications {

    private LedgerEntrySpecifications() {
    }

    static Specification<LedgerEntry> forWallet(UUID walletId) {
        return (root, query, cb) -> cb.equal(root.get("walletId"), walletId);
    }

    static Specification<LedgerEntry> ofType(TransactionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("transaction").get("type"), type);
    }

    static Specification<LedgerEntry> inDirection(EntryDirection direction) {
        return (root, query, cb) -> direction == null ? null : cb.equal(root.get("direction"), direction);
    }

    static Specification<LedgerEntry> createdFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    static Specification<LedgerEntry> createdBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("createdAt"), to);
    }
}
