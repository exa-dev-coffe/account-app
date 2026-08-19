package com.account_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Authentication & Session Endpoints")
class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/1.0/auth/login - Non-existent User or Invalid Credentials (400)")
    void testLoginInvalidCredentials() throws Exception {
        String body = """
                {
                    "email": "nonexistent@gmail.com",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/1.0/auth/google - OAuth Redirect")
    void testGoogleAuthRedirect() throws Exception {
        mockMvc.perform(get("/api/1.0/auth/google"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /api/1.0/auth/google/callback - Invalid Code Redirection")
    void testGoogleAuthCallbackInvalidCode() throws Exception {
        mockMvc.perform(get("/api/1.0/auth/google/callback").param("code", "invalid-code"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/google/login - Invalid Temp Token (4xx)")
    void testGoogleLoginTempTokenInvalid() throws Exception {
        String body = """
                {
                    "tokenTemp": "invalid-temp-token"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/refresh - Invalid Refresh Token (4xx)")
    void testRefreshTokenInvalid() throws Exception {
        String body = """
                {
                    "refreshToken": "invalid-token-string"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/logout - Logout Request")
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
    @DisplayName("POST /api/1.0/auth/register/send-code - Invalid Email (4xx)")
    void testSendVerificationCodeInvalidEmail() throws Exception {
        String body = """
                {
                    "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/1.0/auth/register - Missing Fields (400)")
    void testRegisterValidationFailure() throws Exception {
        String body = """
                {
                    "email": "test@gmail.com",
                    "password": "",
                    "fullName": ""
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
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

    @Test
    @DisplayName("POST /api/1.0/auth/change-password - Invalid Token (4xx)")
    void testChangePasswordInvalidToken() throws Exception {
        String body = """
                {
                    "token": "invalid-token",
                    "password": "newPassword123"
                }
                """;

        mockMvc.perform(post("/api/1.0/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}
