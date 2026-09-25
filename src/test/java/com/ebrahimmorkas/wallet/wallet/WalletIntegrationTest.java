package com.ebrahimmorkas.wallet.wallet;

import com.ebrahimmorkas.wallet.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletIntegrationTest extends IntegrationTest {

    @Test
    void userCanOpenWalletsInSupportedCurrencies() throws Exception {
        String token = newUserToken();

        openWallet(token, "USD");
        openWallet(token, "EUR");

        mockMvc.perform(get("/api/wallets").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].balance").value(0));
    }

    @Test
    void onlyOneWalletPerCurrency() throws Exception {
        String token = newUserToken();
        openWallet(token, "GBP");

        mockMvc.perform(post("/api/wallets").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currency\": \"GBP\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unsupportedCurrencyIsRejected() throws Exception {
        mockMvc.perform(post("/api/wallets").header("Authorization", newUserToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currency\": \"JPY\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY"));
    }

    @Test
    void usersCannotSeeEachOthersWallets() throws Exception {
        String alice = newUserToken();
        String bob = newUserToken();
        String aliceWalletId = openWallet(alice, "USD");

        mockMvc.perform(get("/api/wallets/{id}", aliceWalletId).header("Authorization", alice))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/wallets/{id}", aliceWalletId).header("Authorization", bob))
                .andExpect(status().isNotFound());
    }

    private String openWallet(String token, String currency) throws Exception {
        String body = mockMvc.perform(post("/api/wallets").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\": \"%s\"}".formatted(currency)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("id").asText();
    }
}
