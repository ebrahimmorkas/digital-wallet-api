package com.ebrahimmorkas.wallet.ledger;

import com.ebrahimmorkas.wallet.support.IntegrationTest;
import com.ebrahimmorkas.wallet.support.WalletTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatementIntegrationTest extends IntegrationTest {

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
        mockMvc.perform(post("/api/transfers").header("Authorization", alice)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceWalletId": "%s", "destinationWalletId": "%s", "amount": 30.00, "description": "Dinner"}
                                """.formatted(aliceUsd, bobUsd)))
                .andExpect(status().isCreated());
    }

    @Test
    void statementListsEntriesNewestFirstWithRunningBalance() throws Exception {
        mockMvc.perform(get("/api/wallets/{id}/transactions", aliceUsd).header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(70.00))
                .andExpect(jsonPath("$.content[0].counterpartyWalletId").value(bobUsd))
                .andExpect(jsonPath("$.content[0].description").value("Dinner"))
                .andExpect(jsonPath("$.content[1].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[1].balanceAfter").value(100.00))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void statementCanBeFilteredByTypeAndDirection() throws Exception {
        mockMvc.perform(get("/api/wallets/{id}/transactions", aliceUsd).header("Authorization", alice)
                        .param("type", "DEPOSIT"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"));

        mockMvc.perform(get("/api/wallets/{id}/transactions", aliceUsd).header("Authorization", alice)
                        .param("direction", "CREDIT"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].direction").value("CREDIT"));
    }

    @Test
    void statementCanBeFilteredByDateRangeAndPaged() throws Exception {
        String tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).toString();

        mockMvc.perform(get("/api/wallets/{id}/transactions", aliceUsd).header("Authorization", alice)
                        .param("from", tomorrow))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/wallets/{id}/transactions", aliceUsd).header("Authorization", alice)
                        .param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void invalidDateRangeIsRejected() throws Exception {
        String now = Instant.now().toString();

        mockMvc.perform(get("/api/wallets/{id}/transactions", aliceUsd).header("Authorization", alice)
                        .param("from", now).param("to", now))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void cannotReadAnotherUsersStatement() throws Exception {
        mockMvc.perform(get("/api/wallets/{id}/transactions", aliceUsd).header("Authorization", newUserToken()))
                .andExpect(status().isNotFound());
    }
}
