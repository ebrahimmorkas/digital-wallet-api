package com.ebrahimmorkas.wallet.admin;

import com.ebrahimmorkas.wallet.wallet.Wallet;
import com.ebrahimmorkas.wallet.wallet.WalletStatus;
import com.ebrahimmorkas.wallet.wallet.WalletType;

import java.math.BigDecimal;
import java.util.UUID;

/** Back-office view of a wallet, including its owner. */
public record AdminWalletResponse(UUID id, UUID ownerId, String ownerEmail, String currency, BigDecimal balance,
                                  WalletType type, WalletStatus status) {

    static AdminWalletResponse from(Wallet wallet) {
        return new AdminWalletResponse(wallet.getId(),
                wallet.getOwner() == null ? null : wallet.getOwner().getId(),
                wallet.getOwner() == null ? null : wallet.getOwner().getEmail(),
                wallet.getCurrency(), wallet.getBalance(), wallet.getType(), wallet.getStatus());
    }
}
