package com.ebrahimmorkas.wallet.ledger;

import com.ebrahimmorkas.wallet.common.exception.BusinessRuleException;
import com.ebrahimmorkas.wallet.common.exception.NotFoundException;
import com.ebrahimmorkas.wallet.wallet.Wallet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The only component that moves money. Every movement is a double-entry posting: one DEBIT and one
 * CREDIT of the same amount, so the sum of all entries per currency is always zero.
 *
 * <p>Both wallets are locked with {@code SELECT ... FOR UPDATE} in a globally consistent order
 * (by id). Two concurrent transfers A→B and B→A therefore can't deadlock, and balance checks can't
 * race each other.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final EntityManager entityManager;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public Transaction post(TransactionType type, UUID debitWalletId, UUID creditWalletId, BigDecimal amount,
                            String description, UUID initiatedBy) {
        if (debitWalletId.equals(creditWalletId)) {
            throw new BusinessRuleException("SAME_WALLET", "Source and destination wallets must differ");
        }

        Map<UUID, Wallet> locked = Stream.of(debitWalletId, creditWalletId)
                .sorted(Comparator.naturalOrder())
                .map(this::lockForUpdate)
                .collect(Collectors.toMap(Wallet::getId, Function.identity()));
        Wallet debit = locked.get(debitWalletId);
        Wallet credit = locked.get(creditWalletId);

        if (!debit.getCurrency().equals(credit.getCurrency())) {
            throw new BusinessRuleException("CURRENCY_MISMATCH",
                    "Cannot move %s into a %s wallet".formatted(debit.getCurrency(), credit.getCurrency()));
        }

        debit.debit(amount);
        credit.credit(amount);

        Transaction transaction = transactionRepository.save(new Transaction(type, amount, debit.getCurrency(),
                debitWalletId, creditWalletId, description, initiatedBy));
        ledgerEntryRepository.save(new LedgerEntry(transaction, debitWalletId, EntryDirection.DEBIT, amount,
                debit.getBalance()));
        ledgerEntryRepository.save(new LedgerEntry(transaction, creditWalletId, EntryDirection.CREDIT, amount,
                credit.getBalance()));
        return transaction;
    }

    /**
     * {@code SELECT ... FOR UPDATE} and reload. Refreshing matters: the wallet may already be in the
     * persistence context (e.g. loaded for an ownership check) with a balance read before the lock.
     */
    private Wallet lockForUpdate(UUID walletId) {
        Wallet wallet = entityManager.find(Wallet.class, walletId);
        if (wallet == null) {
            throw new NotFoundException("Wallet " + walletId + " not found");
        }
        entityManager.refresh(wallet, LockModeType.PESSIMISTIC_WRITE);
        return wallet;
    }
}
