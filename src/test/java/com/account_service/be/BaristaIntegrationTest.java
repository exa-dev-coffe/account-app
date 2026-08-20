package com.account_service.be;

import com.account_service.be.account.AccountModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Barista Management Endpoints (Full Coverage)")
class BaristaIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/1.0/barista/list-barista - Admin List Baristas (200)")
    void testListBaristaAdminSuccess() throws Exception {
        AccountModel admin = createTestAccount("adminbarista@gmail.com", "admin");
        String token = createTestJwt(admin);

        mockMvc.perform(get("/api/1.0/barista/list-barista")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/1.0/barista/register-barista - Admin Register Barista (201)")
    void testRegisterBaristaAdminSuccess() throws Exception {
        AccountModel admin = createTestAccount("adminregister@gmail.com", "admin");
        String token = createTestJwt(admin);

        String body = """
                {
                    "email": "newbarista123@gmail.com",
                    "password": "Password123!",
                    "fullName": "New Barista Staff"
                }
                """;

        mockMvc.perform(post("/api/1.0/barista/register-barista")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/1.0/barista - Admin Delete Barista (200)")
    void testDeleteBaristaAdminSuccess() throws Exception {
        AccountModel admin = createTestAccount("admindelete@gmail.com", "admin");
        AccountModel barista = createTestAccount("baristatodelete@gmail.com", "barista");
        String token = createTestJwt(admin);

        mockMvc.perform(delete("/api/1.0/barista")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("userId", String.valueOf(barista.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/1.0/barista/register-barista - Unauthorized (401)")
    void testRegisterBaristaUnauthorized() throws Exception {
        String body = """
                {
                    "email": "barista@gmail.com",
                    "password": "Password123!",
                    "fullName": "New Barista"
                }
                """;

        mockMvc.perform(post("/api/1.0/barista/register-barista")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/1.0/barista/list-barista - Forbidden for Customer Role (403)")
    void testListBaristaForbiddenCustomer() throws Exception {
        AccountModel customer = createTestAccount("customertest@gmail.com", "customer");
        String token = createTestJwt(customer);

        mockMvc.perform(get("/api/1.0/barista/list-barista")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
