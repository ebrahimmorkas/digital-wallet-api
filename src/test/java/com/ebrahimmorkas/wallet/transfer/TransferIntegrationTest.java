package com.ebrahimmorkas.wallet.transfer;

import com.ebrahimmorkas.wallet.support.IntegrationTest;
import com.ebrahimmorkas.wallet.support.WalletTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferIntegrationTest extends IntegrationTest {

    private WalletTestClient wallets;
    private String alice;
    private String bob;
    private String aliceUsd;
    private String bobUsd;

    @BeforeEach
    void setUp() throws Exception {
        wallets = new WalletTestClient(mockMvc, objectMapper);
        alice = newUserToken();
        bob = newUserToken();
        aliceUsd = wallets.openWallet(alice, "USD");
        bobUsd = wallets.openWallet(bob, "USD");
        wallets.deposit(alice, aliceUsd, "100.00");
    }

    @Test
    void transferMovesMoneyBetweenUsers() throws Exception {
        transfer(alice, aliceUsd, bobUsd, "30.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.sourceWalletId").value(aliceUsd))
                .andExpect(jsonPath("$.destinationWalletId").value(bobUsd));

        mockMvc.perform(get("/api/wallets/{id}", aliceUsd).header("Authorization", alice))
                .andExpect(jsonPath("$.balance").value(70.00));
        mockMvc.perform(get("/api/wallets/{id}", bobUsd).header("Authorization", bob))
                .andExpect(jsonPath("$.balance").value(30.00));
    }

    @Test
    void cannotSpendFromSomeoneElsesWallet() throws Exception {
        transfer(bob, aliceUsd, bobUsd, "10.00").andExpect(status().isNotFound());
    }

    @Test
    void cannotOverdraw() throws Exception {
        transfer(alice, aliceUsd, bobUsd, "100.01")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void cannotTransferAcrossCurrencies() throws Exception {
        String bobEur = wallets.openWallet(bob, "EUR");

        transfer(alice, aliceUsd, bobEur, "10.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
    }

    @Test
    void cannotTransferToSameWallet() throws Exception {
        transfer(alice, aliceUsd, aliceUsd, "10.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SAME_WALLET"));
    }

    private ResultActions transfer(String token, String from, String to, String amount) throws Exception {
        return mockMvc.perform(post("/api/transfers").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceWalletId": "%s", "destinationWalletId": "%s", "amount": %s}
                        """.formatted(from, to, amount)));
    }
}
