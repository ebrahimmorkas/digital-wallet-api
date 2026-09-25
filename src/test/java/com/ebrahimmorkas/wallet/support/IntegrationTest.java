package com.ebrahimmorkas.wallet.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for full-stack tests: real PostgreSQL (Testcontainers), real security filter chain.
 * All subclasses share one application context and therefore one database container.
 */
@SpringBootTest(properties = {
        "app.admin.email=" + IntegrationTest.ADMIN_EMAIL,
        "app.admin.password=" + IntegrationTest.PASSWORD
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    protected static final String PASSWORD = "Secret123";
    protected static final String ADMIN_EMAIL = "admin@example.com";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "fullName": "Test User"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    protected String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("accessToken").asText();
    }

    /** Registers a fresh user and returns an {@code Authorization} header value for them. */
    protected String newUserToken() throws Exception {
        String email = uniqueEmail();
        register(email);
        return "Bearer " + login(email);
    }

    protected JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}
