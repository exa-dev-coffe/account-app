package com.account_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Barista Management Endpoints")
class BaristaIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/1.0/barista/register-barista - Unauthorized (401)")
    void testRegisterBaristaUnauthorized() throws Exception {
        String body = """
                {
                    "email": "barista@gmail.com",
                    "password": "password123",
                    "fullName": "New Barista"
                }
                """;

        mockMvc.perform(post("/api/1.0/barista/register-barista")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/1.0/barista/list-barista - Unauthorized (401)")
    void testListBaristaUnauthorized() throws Exception {
        mockMvc.perform(get("/api/1.0/barista/list-barista"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("DELETE /api/1.0/barista - Missing UserId (400)")
    void testDeleteBaristaMissingUserId() throws Exception {
        mockMvc.perform(delete("/api/1.0/barista"))
                .andExpect(status().is4xxClientError());
    }
}
