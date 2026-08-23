package com.account_service.be;

import com.account_service.be.account.AccountModel;
import com.account_service.be.account.dto.CreateUserAdminRequestDto;
import com.account_service.be.utils.PasswordUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User Management PBAC & Admin Endpoints Integration Tests")
class UserManagementIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /api/1.0/admin/users - Admin List Users (200 OK)")
    void testListUsersAdminSuccess() throws Exception {
        AccountModel admin = createTestAccount("adminlistusers@gmail.com", "admin");
        createTestAccount("staffuser1@gmail.com", "barista");
        createTestAccount("staffuser2@gmail.com", "user");
        String token = createTestJwt(admin);

        mockMvc.perform(get("/api/1.0/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.totalData").isNumber());
    }

    @Test
    @DisplayName("GET /api/1.0/admin/users?roleId=3 - Filter Users by Role (200 OK)")
    void testListUsersFilterByRole() throws Exception {
        AccountModel admin = createTestAccount("adminfilterrole@gmail.com", "admin");
        createTestAccount("baristafilter@gmail.com", "barista");
        String token = createTestJwt(admin);

        mockMvc.perform(get("/api/1.0/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("roleId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].roleId").value(3));
    }

    @Test
    @DisplayName("POST /api/1.0/admin/users - Admin Create User with Custom Role (201 Created)")
    void testCreateUserAdminSuccess() throws Exception {
        AccountModel admin = createTestAccount("admincreateuser@gmail.com", "admin");
        String token = createTestJwt(admin);

        String body = """
                {
                    "fullName": "Finance Manager",
                    "email": "finance.staff@diskusicoffee.id",
                    "password": "SecurePassword123!",
                    "roleId": 3
                }
                """;

        mockMvc.perform(post("/api/1.0/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("finance.staff@diskusicoffee.id"))
                .andExpect(jsonPath("$.data.roleId").value(3));

        AccountModel createdAccount = accountRepository.findByEmail("finance.staff@diskusicoffee.id");
        assertNotNull(createdAccount);
        assertEquals("Finance Manager", createdAccount.getFullName());
        assertTrue(PasswordUtils.matches("SecurePassword123!", createdAccount.getPassword()));
    }

    @Test
    @DisplayName("PUT /api/1.0/admin/users/{userId} - Admin Update User and Role (200 OK)")
    void testUpdateUserAdminSuccess() throws Exception {
        AccountModel admin = createTestAccount("adminupdateuser@gmail.com", "admin");
        AccountModel targetUser = createTestAccount("targetupdate@gmail.com", "user");
        String token = createTestJwt(admin);

        String body = """
                {
                    "fullName": "Promoted Staff",
                    "roleId": 3
                }
                """;

        mockMvc.perform(put("/api/1.0/admin/users/" + targetUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Promoted Staff"))
                .andExpect(jsonPath("$.data.roleId").value(3));

        AccountModel updated = accountRepository.findByUserId(targetUser.getUserId());
        assertEquals("Promoted Staff", updated.getFullName());
        assertEquals(3, updated.getRole().getRoleId());
    }

    @Test
    @DisplayName("PUT /api/1.0/admin/users/{userId} - Prevent Admin from Modifying Own Role (400 Bad Request)")
    void testAdminCannotModifyOwnRole() throws Exception {
        AccountModel admin = createTestAccount("adminselfrole@gmail.com", "admin");
        String token = createTestJwt(admin);

        String body = """
                {
                    "fullName": "Admin New Name",
                    "roleId": 2
                }
                """;

        mockMvc.perform(put("/api/1.0/admin/users/" + admin.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot modify your own role"));
    }

    @Test
    @DisplayName("PUT /api/1.0/admin/users/{userId}/password - Admin Direct Password Reset (200 OK)")
    void testAdminResetUserPasswordSuccess() throws Exception {
        AccountModel admin = createTestAccount("adminresetpwd@gmail.com", "admin");
        AccountModel targetUser = createTestAccount("targetresetpwd@gmail.com", "user");
        String token = createTestJwt(admin);

        String body = """
                {
                    "newPassword": "BrandNewPassword2026!"
                }
                """;

        mockMvc.perform(put("/api/1.0/admin/users/" + targetUser.getUserId() + "/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        AccountModel updated = accountRepository.findByUserId(targetUser.getUserId());
        assertTrue(PasswordUtils.matches("BrandNewPassword2026!", updated.getPassword()));
    }

    @Test
    @DisplayName("DELETE /api/1.0/admin/users/{userId} - Admin Delete User (200 OK)")
    void testDeleteUserAdminSuccess() throws Exception {
        AccountModel admin = createTestAccount("admindeleteuser@gmail.com", "admin");
        AccountModel targetUser = createTestAccount("targetdelete@gmail.com", "user");
        String token = createTestJwt(admin);

        mockMvc.perform(delete("/api/1.0/admin/users/" + targetUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        AccountModel deleted = accountRepository.findByUserId(targetUser.getUserId());
        assertNull(deleted);

        // Verify soft delete in SQL database directly (record still exists with deleted_at IS NOT NULL)
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tm_accounts WHERE user_id = ? AND deleted_at IS NOT NULL",
                Integer.class,
                targetUser.getUserId()
        );
        assertEquals(1, count);

        // Verify that the same email can be reused for a new user thanks to partial unique index (WHERE deleted_at IS NULL)
        CreateUserAdminRequestDto recreateReq = CreateUserAdminRequestDto.builder()
                .fullName("Recreated User")
                .email("targetdelete@gmail.com")
                .password("password123")
                .roleId(2)
                .build();

        mockMvc.perform(post("/api/1.0/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recreateReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        AccountModel recreated = accountRepository.findByEmail("targetdelete@gmail.com");
        assertNotNull(recreated);
        assertNotEquals(targetUser.getUserId(), recreated.getUserId());
    }

    @Test
    @DisplayName("DELETE /api/1.0/admin/users/{userId} - Cannot Delete Self (400 Bad Request)")
    void testCannotDeleteSelf() throws Exception {
        AccountModel admin = createTestAccount("admindeleteself@gmail.com", "admin");
        String token = createTestJwt(admin);

        mockMvc.perform(delete("/api/1.0/admin/users/" + admin.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot delete your own account"));
    }

    @Test
    @DisplayName("GET /api/1.0/admin/users - Customer Role is Forbidden (403 Forbidden)")
    void testUserManagementForbiddenForUnauthorizedRole() throws Exception {
        AccountModel customer = createTestAccount("unauthorizedcust@gmail.com", "user");
        String token = createTestJwt(customer);

        mockMvc.perform(get("/api/1.0/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
