package com.account_service.be;

import com.account_service.be.account.AccountModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Authentication & Session Endpoints (Full Coverage + Direct DB Assertions)")
class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/1.0/auth/login - Success (200)")
    void testLoginSuccess() throws Exception {
        createTestAccount("userlogin@gmail.com", "customer");

        String body = """
                {
                    "email": "userlogin@gmail.com",
                    "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/login - Invalid Password (400)")
    void testLoginInvalidPassword() throws Exception {
        createTestAccount("userinvalid@gmail.com", "customer");

        String body = """
                {
                    "email": "userinvalid@gmail.com",
                    "password": "WrongPassword!"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/login - Non-existent User (400)")
    void testLoginNonExistentUser() throws Exception {
        String body = """
                {
                    "email": "nonexistent@gmail.com",
                    "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/register/send-code & register - Success Flow (201 + Direct DB Assertion)")
    void testRegisterSuccessFlow() throws Exception {
        String sendCodeBody = """
                {
                    "email": "newregister@gmail.com"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendCodeBody))
                .andExpect(status().isOk());

        // Set verification code directly in Redis for testing
        redisTemplate.opsForValue().set("register:code:newregister@gmail.com", "123456");

        String registerBody = """
                {
                    "email": "newregister@gmail.com",
                    "password": "Password123!",
                    "fullName": "New Registered User",
                    "code": "123456"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Direct DB Verification
        AccountModel registeredAccount = accountRepository.findByEmail("newregister@gmail.com");
        assertNotNull(registeredAccount);
        assertEquals("New Registered User", registeredAccount.getFullName());
        assertEquals("user", registeredAccount.getRole().getRoleName());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/register - Invalid Verification Code (400)")
    void testRegisterInvalidCode() throws Exception {
        String registerBody = """
                {
                    "email": "wrongcode@gmail.com",
                    "password": "Password123!",
                    "fullName": "Wrong Code User",
                    "code": "000000"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/logout - Success (200)")
    void testLogout() throws Exception {
        String body = """
                {
                    "refreshToken": "sample-refresh-token"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/1.0/auth/google - OAuth Redirect (3xx)")
    void testGoogleAuthRedirect() throws Exception {
        mockMvc.perform(get("/api/1.0/auth/google"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/apple/events - Webhook Handler (200)")
    void testAppleEventsWebhook() throws Exception {
        String webhookBody = """
                {
                    "payload": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwiYXVkIjoiZHVtbXktYXBwbGUtaWQiLCJpYXQiOjE2MDAwMDAwMDAsImp0aSI6ImV2dC0xMjMiLCJldmVudHMiOiJ7XCJ0eXBlXCI6XCJjb25zZW50LXJldm9rZWRcIixcInN1YlwiOlwiYXBwbGUtc3ViLTEyM1wifSJ9.signature"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/apple/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/1.0/auth/apple/unbind - Rejection for Private Relay Email without valid setup (400)")
    void testUnbindAppleWithRelayEmailFails() throws Exception {
        AccountModel relayAccount = createTestAccount("user@privaterelay.appleid.com", "customer");
        relayAccount.setAppleSub("apple-sub-relay-123");
        relayAccount.setAppleEmail("user@privaterelay.appleid.com");
        accountRepository.save(relayAccount);

        String jwt = createTestJwt(relayAccount);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/1.0/auth/apple/unbind")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("private relay email")));
    }

    @Test
    @DisplayName("DELETE /api/1.0/auth/apple/unbind - Success with Standard Email (200)")
    void testUnbindAppleSuccess() throws Exception {
        AccountModel normalAccount = createTestAccount("standarduser@gmail.com", "customer");
        normalAccount.setAppleSub("apple-sub-standard-123");
        normalAccount.setAppleEmail("standarduser@gmail.com");
        accountRepository.save(normalAccount);

        String jwt = createTestJwt(normalAccount);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/1.0/auth/apple/unbind")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        AccountModel updated = accountRepository.findByUserId(normalAccount.getUserId());
        org.junit.jupiter.api.Assertions.assertNull(updated.getAppleSub());
        org.junit.jupiter.api.Assertions.assertNull(updated.getAppleEmail());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/forgot-password - Invalid Email Format (400)")
    void testForgotPasswordInvalidEmail() throws Exception {
        String body = """
                {
                    "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
