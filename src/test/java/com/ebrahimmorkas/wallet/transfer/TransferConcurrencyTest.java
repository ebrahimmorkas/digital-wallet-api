package com.ebrahimmorkas.wallet.transfer;

import com.ebrahimmorkas.wallet.common.exception.BusinessRuleException;
import com.ebrahimmorkas.wallet.support.IntegrationTest;
import com.ebrahimmorkas.wallet.support.WalletTestClient;
import com.ebrahimmorkas.wallet.wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hammers the ledger with concurrent transfers against a real PostgreSQL database to prove the
 * locking strategy: no deadlocks, no lost updates, no overdrafts, and the books always balance.
 */
class TransferConcurrencyTest extends IntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void opposingConcurrentTransfersNeitherDeadlockNorLoseUpdates() throws Exception {
        WalletTestClient wallets = new WalletTestClient(mockMvc, objectMapper);
        String alice = newUserToken();
        String bob = newUserToken();
        UUID aliceWallet = UUID.fromString(wallets.openWallet(alice, "USD"));
        UUID bobWallet = UUID.fromString(wallets.openWallet(bob, "USD"));
        wallets.deposit(alice, aliceWallet.toString(), "1000.00");
        wallets.deposit(bob, bobWallet.toString(), "1000.00");

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(transfer(userId(alice), aliceWallet, bobWallet, "1.00"));
            tasks.add(transfer(userId(bob), bobWallet, aliceWallet, "1.00"));
        }
        List<Object> results = runConcurrently(tasks);

        assertThat(results).allSatisfy(result -> assertThat(result).isNotInstanceOf(Throwable.class));
        assertThat(balanceOf(aliceWallet)).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(bobWallet)).isEqualByComparingTo("1000.00");
        assertLedgerBalances();
    }

    @Test
    void concurrentSpendingCannotOverdrawAWallet() throws Exception {
        WalletTestClient wallets = new WalletTestClient(mockMvc, objectMapper);
        String alice = newUserToken();
        String bob = newUserToken();
        UUID aliceWallet = UUID.fromString(wallets.openWallet(alice, "EUR"));
        UUID bobWallet = UUID.fromString(wallets.openWallet(bob, "EUR"));
        wallets.deposit(alice, aliceWallet.toString(), "10.00");

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            tasks.add(transfer(userId(alice), aliceWallet, bobWallet, "1.00"));
        }
        List<Object> results = runConcurrently(tasks);

        assertThat(results).filteredOn(r -> !(r instanceof Throwable)).hasSize(10);
        assertThat(results).filteredOn(r -> r instanceof BusinessRuleException).hasSize(15);
        assertThat(balanceOf(aliceWallet)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(bobWallet)).isEqualByComparingTo("10.00");
        assertLedgerBalances();
    }

    private Callable<Object> transfer(UUID userId, UUID from, UUID to, String amount) {
        return () -> transferService.transfer(userId, new TransferRequest(from, to, new BigDecimal(amount), null));
    }

    /** Releases all tasks at the same instant and collects either the result or the thrown exception. */
    private static List<Object> runConcurrently(List<Callable<Object>> tasks) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(16)) {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        return task.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }

    private UUID userId(String bearerToken) {
        return UUID.fromString(jwtDecoder.decode(bearerToken.substring("Bearer ".length())).getSubject());
    }

    private BigDecimal balanceOf(UUID walletId) {
        return walletRepository.findById(walletId).orElseThrow().getBalance();
    }

    private void assertLedgerBalances() {
        List<BigDecimal> imbalances = jdbcTemplate.queryForList("""
                select sum(case when e.direction = 'CREDIT' then e.amount else -e.amount end)
                from ledger_entries e join wallets w on w.id = e.wallet_id
                group by w.currency""", BigDecimal.class);
        assertThat(imbalances).allSatisfy(sum -> assertThat(sum).isEqualByComparingTo("0"));
        // Stored balances must equal the balances implied by the ledger
        Integer drifted = jdbcTemplate.queryForObject("""
                select count(*) from wallets w
                where w.balance <> coalesce((select sum(case when e.direction = 'CREDIT' then e.amount else -e.amount end)
                                             from ledger_entries e where e.wallet_id = w.id), 0)""", Integer.class);
        assertThat(drifted).isZero();
    }
}
