package com.ebrahimmorkas.wallet.ledger;

import com.ebrahimmorkas.wallet.wallet.Wallet;
import com.ebrahimmorkas.wallet.wallet.WalletRepository;
import com.ebrahimmorkas.wallet.wallet.WalletService;
import com.ebrahimmorkas.wallet.wallet.WalletType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Moves money between a user's wallet and the outside world. The currency's SYSTEM wallet is the
 * counter-account, so deposits and withdrawals are double-entry postings like any transfer.
 */
@Service
@RequiredArgsConstructor
public class FundingService {

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;

    @Transactional
    public TransactionResponse deposit(UUID userId, UUID walletId, AmountRequest request) {
        Wallet wallet = walletService.getOwnedWallet(userId, walletId);
        Transaction transaction = ledgerService.post(TransactionType.DEPOSIT, systemWalletId(wallet), walletId,
                request.amount(), request.description(), userId);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(UUID userId, UUID walletId, AmountRequest request) {
        Wallet wallet = walletService.getOwnedWallet(userId, walletId);
        Transaction transaction = ledgerService.post(TransactionType.WITHDRAWAL, walletId, systemWalletId(wallet),
                request.amount(), request.description(), userId);
        return TransactionResponse.from(transaction);
    }

    private UUID systemWalletId(Wallet wallet) {
        return walletRepository.findByTypeAndCurrency(WalletType.SYSTEM, wallet.getCurrency())
                .orElseThrow(() -> new IllegalStateException("No system wallet for " + wallet.getCurrency()))
                .getId();
    }
}
