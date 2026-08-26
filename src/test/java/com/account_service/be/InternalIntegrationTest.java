package com.account_service.be;

import com.account_service.be.account.AccountModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Internal Service Endpoints (Full Coverage)")
class InternalIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/internal/name-users - Success (200)")
    void testInternalGetNameUsersSuccess() throws Exception {
        AccountModel account = createTestAccount("internaluser@gmail.com", "customer");

        String query = "ids=" + account.getUserId();
        String timestamp = Instant.now().toString();
        String signature = createHmacSignature(query, timestamp, "");

        mockMvc.perform(get("/api/internal/name-users?" + query)
                        .header("X-Signature", signature)
                        .header("X-Timestamp", timestamp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success Get Names"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].userId").value(account.getUserId()))
                .andExpect(jsonPath("$.data[0].fullName").value(account.getFullName()))
                .andExpect(jsonPath("$.data[0].email").value(account.getEmail()));
    }

    @Test
    @DisplayName("GET /api/internal/name-users - Missing Signature or Timestamp (401)")
    void testInternalGetNameUsersMissingHeaders() throws Exception {
        mockMvc.perform(get("/api/internal/name-users?ids=1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/internal/name-users - Invalid Signature (401)")
    void testInternalGetNameUsersInvalidSignature() throws Exception {
        String timestamp = Instant.now().toString();

        mockMvc.perform(get("/api/internal/name-users?ids=1")
                        .header("X-Signature", "invalid-signature-hash")
                        .header("X-Timestamp", timestamp))
                .andExpect(status().is4xxClientError());
    }
}
