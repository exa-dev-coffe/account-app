package com.account_service.be;

import com.account_service.be.account.AccountModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User Profile Endpoints (Full Coverage + Direct DB Assertions)")
class ProfileIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/1.0/me - Success (200)")
    void testMeSuccess() throws Exception {
        AccountModel account = createTestAccount("myprofile@gmail.com", "customer");
        String token = createTestJwt(account);

        mockMvc.perform(get("/api/1.0/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("myprofile@gmail.com"))
                .andExpect(jsonPath("$.data.fullName").value(account.getFullName()));
    }

    @Test
    @DisplayName("GET /api/1.0/me - Unauthorized (401)")
    void testMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/1.0/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("PATCH /api/1.0/update-profile - Success (200 + Direct DB Assertion)")
    void testUpdateProfileSuccess() throws Exception {
        AccountModel account = createTestAccount("updateprofile@gmail.com", "customer");
        String token = createTestJwt(account);

        String body = """
                {
                    "fullName": "Updated User Name",
                    "photo": "http://example.com/photo.jpg"
                }
                """;

        mockMvc.perform(patch("/api/1.0/update-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Direct DB Verification
        AccountModel updatedAccount = accountRepository.findByEmail("updateprofile@gmail.com");
        assertNotNull(updatedAccount);
        assertEquals("Updated User Name", updatedAccount.getFullName());
        assertEquals("http://example.com/photo.jpg", updatedAccount.getPhoto());
    }

    @Test
    @DisplayName("PATCH /api/1.0/update-profile - Unauthorized (401)")
    void testUpdateProfileUnauthorized() throws Exception {
        String body = """
                {
                    "fullName": "Updated Name",
                    "photo": "http://example.com/photo.jpg"
                }
                """;

        mockMvc.perform(patch("/api/1.0/update-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}
