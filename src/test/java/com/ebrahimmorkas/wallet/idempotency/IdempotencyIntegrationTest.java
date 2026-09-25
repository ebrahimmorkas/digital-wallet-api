package com.ebrahimmorkas.wallet.idempotency;

import com.ebrahimmorkas.wallet.ledger.TransactionResponse;
import com.ebrahimmorkas.wallet.support.IntegrationTest;
import com.ebrahimmorkas.wallet.support.WalletTestClient;
import com.ebrahimmorkas.wallet.transfer.TransferRequest;
import com.ebrahimmorkas.wallet.transfer.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdempotencyIntegrationTest extends IntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String alice;
    private String aliceUsd;
    private String bobUsd;

    @BeforeEach
    void setUp() throws Exception {
        WalletTestClient wallets = new WalletTestClient(mockMvc, objectMapper);
        alice = newUserToken();
        aliceUsd = wallets.openWallet(alice, "USD");
        bobUsd = wallets.openWallet(newUserToken(), "USD");
        wallets.deposit(alice, aliceUsd, "100.00");
    }

    @Test
    void retryWithSameKeyReplaysOriginalTransactionWithoutPayingTwice() throws Exception {
        String key = UUID.randomUUID().toString();

        String first = transfer(key, "25.00")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andReturn().getResponse().getContentAsString();
        String retry = transfer(key, "25.00")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat(json(retry).get("id")).isEqualTo(json(first).get("id"));
        mockMvc.perform(get("/api/wallets/{id}", aliceUsd).header("Authorization", alice))
                .andExpect(jsonPath("$.balance").value(75.00));
    }

    @Test
    void reusingKeyForDifferentRequestIsRejected() throws Exception {
        String key = UUID.randomUUID().toString();
        transfer(key, "10.00").andExpect(status().isCreated());

        transfer(key, "99.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void failedRequestReleasesTheKeySoItCanBeRetried() throws Exception {
        String key = UUID.randomUUID().toString();

        transfer(key, "500.00").andExpect(status().isUnprocessableEntity());
        new WalletTestClient(mockMvc, objectMapper).deposit(alice, aliceUsd, "400.00");

        transfer(key, "500.00")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"));
    }

    @Test
    void idempotencyKeyHeaderIsRequired() throws Exception {
        mockMvc.perform(post("/api/transfers").header("Authorization", alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentRetriesWithSameKeyExecuteExactlyOnce() throws Exception {
        UUID userId = UUID.fromString(jwtDecoder.decode(alice.substring("Bearer ".length())).getSubject());
        TransferRequest request = new TransferRequest(UUID.fromString(aliceUsd), UUID.fromString(bobUsd),
                new BigDecimal("5.00"), null);
        String key = UUID.randomUUID().toString();

        CountDownLatch start = new CountDownLatch(1);
        List<Future<IdempotentResult<TransactionResponse>>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return idempotencyService.execute(userId, key, request.fingerprint(),
                            () -> transferService.transfer(userId, request));
                }));
            }
            start.countDown();
            List<IdempotentResult<TransactionResponse>> results = new ArrayList<>();
            for (var future : futures) {
                results.add(future.get());
            }

            assertThat(results).extracting(r -> r.body().id()).containsOnly(results.getFirst().body().id());
            assertThat(results).filteredOn(r -> !r.replayed()).hasSize(1);
        }
        Integer transfers = jdbcTemplate.queryForObject(
                "select count(*) from transactions where type = 'TRANSFER' and debit_wallet_id = ?::uuid",
                Integer.class, aliceUsd);
        assertThat(transfers).isEqualTo(1);
    }

    private ResultActions transfer(String key, String amount) throws Exception {
        return mockMvc.perform(post("/api/transfers").header("Authorization", alice)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(amount)));
    }

    private String body(String amount) {
        return """
                {"sourceWalletId": "%s", "destinationWalletId": "%s", "amount": %s}
                """.formatted(aliceUsd, bobUsd, amount);
    }
}
