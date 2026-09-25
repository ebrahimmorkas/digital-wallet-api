package com.ebrahimmorkas.wallet.admin;

import com.ebrahimmorkas.wallet.support.IntegrationTest;
import com.ebrahimmorkas.wallet.support.WalletTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminWalletIntegrationTest extends IntegrationTest {

    private String admin;
    private String user;
    private String userWallet;

    @BeforeEach
    void setUp() throws Exception {
        admin = "Bearer " + login(ADMIN_EMAIL);
        user = newUserToken();
        userWallet = new WalletTestClient(mockMvc, objectMapper).openWallet(user, "USD");
        new WalletTestClient(mockMvc, objectMapper).deposit(user, userWallet, "50.00");
    }

    @Test
    void adminCanInspectAnyWallet() throws Exception {
        mockMvc.perform(get("/api/admin/wallets/{id}", userWallet).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00))
                .andExpect(jsonPath("$.ownerEmail").exists());
    }

    @Test
    void frozenWalletCannotMoveMoneyUntilUnfrozen() throws Exception {
        mockMvc.perform(post("/api/admin/wallets/{id}/freeze", userWallet).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        withdraw().andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("WALLET_FROZEN"));

        mockMvc.perform(post("/api/admin/wallets/{id}/unfreeze", userWallet).header("Authorization", admin))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        withdraw().andExpect(status().isCreated());
    }

    @Test
    void regularUsersCannotUseAdminEndpoints() throws Exception {
        mockMvc.perform(post("/api/admin/wallets/{id}/freeze", userWallet).header("Authorization", user))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/wallets/{id}", userWallet).header("Authorization", user))
                .andExpect(status().isForbidden());
    }

    private ResultActions withdraw() throws Exception {
        return mockMvc.perform(post("/api/wallets/{id}/withdrawals", userWallet).header("Authorization", user)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 5.00}"));
    }
}
