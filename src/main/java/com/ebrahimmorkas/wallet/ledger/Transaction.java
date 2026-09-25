package com.ebrahimmorkas.wallet.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** An immutable record of one money movement. Its two {@link LedgerEntry} rows always balance. */
@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "debit_wallet_id", nullable = false)
    private UUID debitWalletId;

    @Column(name = "credit_wallet_id", nullable = false)
    private UUID creditWalletId;

    private String description;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    Transaction(TransactionType type, BigDecimal amount, String currency, UUID debitWalletId, UUID creditWalletId,
                String description, UUID initiatedBy) {
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.debitWalletId = debitWalletId;
        this.creditWalletId = creditWalletId;
        this.description = description;
        this.initiatedBy = initiatedBy;
    }
}
