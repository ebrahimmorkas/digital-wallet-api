package com.ebrahimmorkas.wallet.ledger;

import com.ebrahimmorkas.wallet.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FundingIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void depositThenWithdrawUpdatesBalanceAndWritesLedgerEntries() throws Exception {
        String token = newUserToken();
        String walletId = openWallet(token, "USD");

        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId).header("Authorization", token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 150.00, \"description\": \"Card top-up\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.destinationWalletId").value(walletId));

        mockMvc.perform(post("/api/wallets/{id}/withdrawals", walletId).header("Authorization", token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 40.25}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));

        mockMvc.perform(get("/api/wallets/{id}", walletId).header("Authorization", token))
                .andExpect(jsonPath("$.balance").value(109.75));

        List<BigDecimal> balances = jdbcTemplate.queryForList(
                "select balance_after from ledger_entries where wallet_id = ?::uuid order by id", BigDecimal.class, walletId);
        assertThat(balances).extracting(BigDecimal::toPlainString).containsExactly("150.00", "109.75");
    }

    @Test
    void cannotWithdrawMoreThanBalance() throws Exception {
        String token = newUserToken();
        String walletId = openWallet(token, "EUR");

        mockMvc.perform(post("/api/wallets/{id}/withdrawals", walletId).header("Authorization", token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void cannotDepositIntoSomeoneElsesWallet() throws Exception {
        String walletId = openWallet(newUserToken(), "USD");

        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId).header("Authorization", newUserToken())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 10.00}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void amountsMustBePositiveWithAtMostTwoDecimals() throws Exception {
        String token = newUserToken();
        String walletId = openWallet(token, "USD");

        for (String amount : List.of("0", "-5", "10.001")) {
            mockMvc.perform(post("/api/wallets/{id}/deposits", walletId).header("Authorization", token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": %s}".formatted(amount)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void ledgerAlwaysBalancesPerCurrency() throws Exception {
        String token = newUserToken();
        String walletId = openWallet(token, "GBP");
        mockMvc.perform(post("/api/wallets/{id}/deposits", walletId).header("Authorization", token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 75.00}"));

        List<BigDecimal> imbalances = jdbcTemplate.queryForList("""
                select sum(case when e.direction = 'CREDIT' then e.amount else -e.amount end)
                from ledger_entries e join wallets w on w.id = e.wallet_id
                group by w.currency""", BigDecimal.class);
        assertThat(imbalances).allSatisfy(sum -> assertThat(sum).isEqualByComparingTo("0"));
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
