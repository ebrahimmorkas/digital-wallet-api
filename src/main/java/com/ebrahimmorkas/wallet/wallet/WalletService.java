package com.ebrahimmorkas.wallet.wallet;

import com.ebrahimmorkas.wallet.common.exception.BusinessRuleException;
import com.ebrahimmorkas.wallet.common.exception.ConflictException;
import com.ebrahimmorkas.wallet.common.exception.NotFoundException;
import com.ebrahimmorkas.wallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Transactional
    public WalletResponse open(UUID userId, String currency) {
        if (!walletRepository.existsByTypeAndCurrency(WalletType.SYSTEM, currency)) {
            throw new BusinessRuleException("UNSUPPORTED_CURRENCY", "Currency " + currency + " is not supported");
        }
        if (walletRepository.existsByOwnerIdAndCurrency(userId, currency)) {
            throw new ConflictException("You already have a " + currency + " wallet");
        }
        Wallet wallet = Wallet.openFor(userRepository.getReferenceById(userId), currency);
        return WalletResponse.from(walletRepository.save(wallet));
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> findMine(UUID userId) {
        return walletRepository.findByOwnerIdOrderByCreatedAt(userId).stream().map(WalletResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WalletResponse findMine(UUID userId, UUID walletId) {
        return WalletResponse.from(getOwnedWallet(userId, walletId));
    }

    /**
     * Returns the wallet only if the caller owns it. Other users' wallets are reported as "not found"
     * rather than "forbidden" so wallet ids cannot be probed.
     */
    @Transactional(readOnly = true)
    public Wallet getOwnedWallet(UUID userId, UUID walletId) {
        return walletRepository.findById(walletId)
                .filter(wallet -> wallet.isOwnedBy(userId))
                .orElseThrow(() -> new NotFoundException("Wallet " + walletId + " not found"));
    }
}
