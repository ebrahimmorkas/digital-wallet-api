package com.ebrahimmorkas.wallet.transfer;

import com.ebrahimmorkas.wallet.common.exception.NotFoundException;
import com.ebrahimmorkas.wallet.ledger.LedgerService;
import com.ebrahimmorkas.wallet.ledger.TransactionResponse;
import com.ebrahimmorkas.wallet.ledger.TransactionType;
import com.ebrahimmorkas.wallet.wallet.WalletRepository;
import com.ebrahimmorkas.wallet.wallet.WalletService;
import com.ebrahimmorkas.wallet.wallet.WalletType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final LedgerService ledgerService;

    /** Sends money from one of the caller's wallets to any other user's wallet in the same currency. */
    @Transactional
    public TransactionResponse transfer(UUID userId, TransferRequest request) {
        walletService.getOwnedWallet(userId, request.sourceWalletId());
        walletRepository.findById(request.destinationWalletId())
                .filter(wallet -> wallet.getType() == WalletType.USER)
                .orElseThrow(() -> new NotFoundException("Wallet " + request.destinationWalletId() + " not found"));

        return TransactionResponse.from(ledgerService.post(TransactionType.TRANSFER, request.sourceWalletId(),
                request.destinationWalletId(), request.amount(), request.description(), userId));
    }
}
