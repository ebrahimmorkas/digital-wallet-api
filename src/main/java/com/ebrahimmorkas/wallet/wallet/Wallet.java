package com.ebrahimmorkas.wallet.wallet;

import com.ebrahimmorkas.wallet.common.exception.BusinessRuleException;
import com.ebrahimmorkas.wallet.user.User;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Version
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Wallet openFor(User owner, String currency) {
        Wallet wallet = new Wallet();
        wallet.owner = owner;
        wallet.currency = currency;
        wallet.type = WalletType.USER;
        wallet.status = WalletStatus.ACTIVE;
        return wallet;
    }

    public boolean isOwnedBy(UUID userId) {
        return owner != null && owner.getId().equals(userId);
    }

    public void debit(BigDecimal amount) {
        assertActive();
        if (type == WalletType.USER && balance.compareTo(amount) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_FUNDS", "Insufficient funds in wallet " + id);
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        assertActive();
        balance = balance.add(amount);
    }

    public void freeze() {
        status = WalletStatus.FROZEN;
    }

    public void unfreeze() {
        status = WalletStatus.ACTIVE;
    }

    private void assertActive() {
        if (status != WalletStatus.ACTIVE) {
            throw new BusinessRuleException("WALLET_FROZEN", "Wallet " + id + " is frozen");
        }
    }
}
