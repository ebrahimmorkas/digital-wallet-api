package com.ebrahimmorkas.wallet.admin;

import com.ebrahimmorkas.wallet.common.exception.BusinessRuleException;
import com.ebrahimmorkas.wallet.common.exception.NotFoundException;
import com.ebrahimmorkas.wallet.wallet.Wallet;
import com.ebrahimmorkas.wallet.wallet.WalletRepository;
import com.ebrahimmorkas.wallet.wallet.WalletType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminWalletService {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public AdminWalletResponse find(UUID walletId) {
        return AdminWalletResponse.from(getUserWallet(walletId));
    }

    @Transactional
    public AdminWalletResponse freeze(UUID adminId, UUID walletId) {
        Wallet wallet = getUserWallet(walletId);
        wallet.freeze();
        log.warn("AUDIT admin={} action=FREEZE wallet={}", adminId, walletId);
        return AdminWalletResponse.from(wallet);
    }

    @Transactional
    public AdminWalletResponse unfreeze(UUID adminId, UUID walletId) {
        Wallet wallet = getUserWallet(walletId);
        wallet.unfreeze();
        log.warn("AUDIT admin={} action=UNFREEZE wallet={}", adminId, walletId);
        return AdminWalletResponse.from(wallet);
    }

    private Wallet getUserWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet " + walletId + " not found"));
        if (wallet.getType() == WalletType.SYSTEM) {
            throw new BusinessRuleException("SYSTEM_WALLET", "System wallets cannot be managed through this API");
        }
        return wallet;
    }
}
