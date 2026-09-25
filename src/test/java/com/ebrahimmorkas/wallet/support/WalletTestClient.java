package com.ebrahimmorkas.wallet.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Small fluent helper for setting up wallets and balances in integration tests. */
public record WalletTestClient(MockMvc mockMvc, ObjectMapper objectMapper) {

    public String openWallet(String token, String currency) throws Exception {
        String body = mockMvc.perform(post("/api/wallets").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\": \"%s\"}".formatted(currency)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    public void deposit(String token, String walletId, String amount) throws Exception {
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId).header("Authorization", token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": %s}".formatted(amount)))
                .andExpect(status().isCreated());
    }
}
