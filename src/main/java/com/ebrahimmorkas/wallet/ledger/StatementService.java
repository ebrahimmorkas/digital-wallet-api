package com.ebrahimmorkas.wallet.ledger;

import com.ebrahimmorkas.wallet.common.exception.BusinessRuleException;
import com.ebrahimmorkas.wallet.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.ebrahimmorkas.wallet.ledger.LedgerEntrySpecifications.createdBefore;
import static com.ebrahimmorkas.wallet.ledger.LedgerEntrySpecifications.createdFrom;
import static com.ebrahimmorkas.wallet.ledger.LedgerEntrySpecifications.forWallet;
import static com.ebrahimmorkas.wallet.ledger.LedgerEntrySpecifications.inDirection;
import static com.ebrahimmorkas.wallet.ledger.LedgerEntrySpecifications.ofType;

@Service
@RequiredArgsConstructor
public class StatementService {

    /** Newest first; id breaks ties between entries written in the same instant. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final WalletService walletService;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public Page<StatementEntryResponse> statement(UUID userId, UUID walletId, StatementFilter filter, Pageable pageable) {
        walletService.getOwnedWallet(userId, walletId);
        if (filter.from() != null && filter.to() != null && !filter.from().isBefore(filter.to())) {
            throw new BusinessRuleException("INVALID_DATE_RANGE", "'from' must be before 'to'");
        }

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        return ledgerEntryRepository.findAll(
                        forWallet(walletId)
                                .and(ofType(filter.type()))
                                .and(inDirection(filter.direction()))
                                .and(createdFrom(filter.from()))
                                .and(createdBefore(filter.to())),
                        sorted)
                .map(StatementEntryResponse::from);
    }

    public record StatementFilter(TransactionType type, EntryDirection direction, Instant from, Instant to) {
    }
}
