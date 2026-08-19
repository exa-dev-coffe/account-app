package com.account_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Internal Service Endpoints")
class InternalIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/internal/name-users - Missing Signature or Params (400/401)")
    void testInternalGetNameUsersInvalid() throws Exception {
        mockMvc.perform(get("/api/internal/name-users"))
                .andExpect(status().is4xxClientError());
    }
}
