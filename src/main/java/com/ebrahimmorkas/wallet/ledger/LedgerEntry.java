package com.ebrahimmorkas.wallet.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One side of a transaction on one wallet. Entries are append-only: never updated or deleted. */
@Entity
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", updatable = false)
    private Transaction transaction;

    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    LedgerEntry(Transaction transaction, UUID walletId, EntryDirection direction, BigDecimal amount,
                BigDecimal balanceAfter) {
        this.transaction = transaction;
        this.walletId = walletId;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }
}
