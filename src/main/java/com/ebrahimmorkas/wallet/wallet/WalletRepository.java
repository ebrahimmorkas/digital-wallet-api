package com.ebrahimmorkas.wallet.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByOwnerIdOrderByCreatedAt(UUID ownerId);

    boolean existsByOwnerIdAndCurrency(UUID ownerId, String currency);

    Optional<Wallet> findByTypeAndCurrency(WalletType type, String currency);

    boolean existsByTypeAndCurrency(WalletType type, String currency);
}
