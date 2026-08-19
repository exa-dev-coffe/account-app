package com.account_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User Profile Endpoints")
class ProfileIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/1.0/me - Unauthorized (401)")
    void testMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/1.0/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("PATCH /api/1.0/update-profile - Unauthorized (401)")
    void testUpdateProfileUnauthorized() throws Exception {
        String body = """
                {
                    "fullName": "Updated Name"
                }
                """;

        mockMvc.perform(patch("/api/1.0/update-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}
