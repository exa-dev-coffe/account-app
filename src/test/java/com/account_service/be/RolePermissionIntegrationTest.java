package com.account_service.be;

import com.account_service.be.account.AccountModel;
import com.account_service.be.roleFeature.dto.CreateRoleRequestDto;
import com.account_service.be.roleFeature.dto.RoleFeaturePermissionItemDto;
import com.account_service.be.roleFeature.dto.UpdateRolePermissionRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Role & Feature Permission Management (PBAC Integration Tests)")
class RolePermissionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/1.0/admin/features - Admin can list all features (200)")
    void testGetFeaturesSuccess() throws Exception {
        AccountModel admin = createTestAccount("roleadmin_feat@gmail.com", "admin");
        String token = createTestJwt(admin);

        mockMvc.perform(get("/api/1.0/admin/features")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(11));
    }

    @Test
    @DisplayName("GET /api/1.0/admin/roles - Admin can list all roles (200)")
    void testGetRolesSuccess() throws Exception {
        AccountModel admin = createTestAccount("roleadmin_roles@gmail.com", "admin");
        String token = createTestJwt(admin);

        mockMvc.perform(get("/api/1.0/admin/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/1.0/admin/roles/{roleId}/permissions - Admin can get role permissions matrix (200)")
    void testGetRolePermissionsSuccess() throws Exception {
        AccountModel admin = createTestAccount("roleadmin_perm@gmail.com", "admin");
        String token = createTestJwt(admin);

        mockMvc.perform(get("/api/1.0/admin/roles/3/permissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleId").value(3))
                .andExpect(jsonPath("$.data.roleName").value("barista"))
                .andExpect(jsonPath("$.data.features").isArray());
    }

    @Test
    @DisplayName("PUT /api/1.0/admin/roles/3/permissions - Admin can update barista permissions (200)")
    void testUpdateRolePermissionsSuccess() throws Exception {
        AccountModel admin = createTestAccount("roleadmin_update@gmail.com", "admin");
        String token = createTestJwt(admin);

        UpdateRolePermissionRequestDto requestDto = new UpdateRolePermissionRequestDto();
        RoleFeaturePermissionItemDto item = new RoleFeaturePermissionItemDto();
        item.setFeatureId(1); // catalog
        item.setCanView(true);
        item.setCanCreate(true);
        item.setCanEdit(true);
        item.setCanDelete(false);
        requestDto.setPermissions(List.of(item));

        mockMvc.perform(put("/api/1.0/admin/roles/3/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleId").value(3));
    }

    @Test
    @DisplayName("PUT /api/1.0/admin/roles/1/permissions - Reject modifying Super Admin role (400)")
    void testUpdateSuperAdminPermissionsForbidden() throws Exception {
        AccountModel admin = createTestAccount("roleadmin_lock@gmail.com", "admin");
        String token = createTestJwt(admin);

        UpdateRolePermissionRequestDto requestDto = new UpdateRolePermissionRequestDto();
        RoleFeaturePermissionItemDto item = new RoleFeaturePermissionItemDto();
        item.setFeatureId(1);
        item.setCanView(false);
        requestDto.setPermissions(List.of(item));

        mockMvc.perform(put("/api/1.0/admin/roles/1/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/1.0/admin/roles - Admin can create a new role (201)")
    void testCreateNewRoleSuccess() throws Exception {
        AccountModel admin = createTestAccount("roleadmin_create@gmail.com", "admin");
        String token = createTestJwt(admin);

        CreateRoleRequestDto requestDto = new CreateRoleRequestDto();
        requestDto.setRoleName("cashier_" + System.currentTimeMillis());

        mockMvc.perform(post("/api/1.0/admin/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleId").isNumber());
    }

    @Test
    @DisplayName("GET /api/1.0/admin/features - Non-admin user is rejected (403)")
    void testNonAdminAccessForbidden() throws Exception {
        AccountModel barista = createTestAccount("barista_unauth@gmail.com", "barista");
        String token = createTestJwt(barista);

        mockMvc.perform(get("/api/1.0/admin/features")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/internal/roles/{roleId}/permissions - Internal HMAC request success (200)")
    void testInternalGetRolePermissionsSuccess() throws Exception {
        String timestamp = Instant.now().toString();
        String signature = createHmacSignature("", timestamp, "");

        mockMvc.perform(get("/api/internal/roles/3/permissions")
                        .header("X-Signature", signature)
                        .header("X-Timestamp", timestamp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap());
    }
}
