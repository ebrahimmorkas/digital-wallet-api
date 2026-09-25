package com.ebrahimmorkas.wallet.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long>, JpaSpecificationExecutor<LedgerEntry> {

    /** Fetches each entry's transaction in the same query to avoid N+1 selects when rendering statements. */
    @Override
    @EntityGraph(attributePaths = "transaction")
    Page<LedgerEntry> findAll(Specification<LedgerEntry> spec, Pageable pageable);
}
