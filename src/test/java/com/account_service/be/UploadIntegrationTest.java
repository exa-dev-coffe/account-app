package com.account_service.be;

import com.account_service.be.account.AccountModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Profile Avatar Upload Endpoints (Real MinIO Testcontainer)")
class UploadIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /upload/upload-profile - Upload Avatar Real MinIO Success (200)")
    void testUploadProfileSuccess() throws Exception {
        AccountModel account = createTestAccount("uploaduser@gmail.com", "customer");
        String token = createTestJwt(account);

        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "fake-image-binary-content".getBytes());
        mockMvc.perform(multipart("/api/1.0/upload/upload-profile")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").exists());
    }

    @Test
    @DisplayName("DELETE /upload/delete-profile - Delete Avatar Real MinIO Success (200)")
    void testDeleteProfileSuccess() throws Exception {
        AccountModel account = createTestAccount("deleteavataruser@gmail.com", "customer");
        String token = createTestJwt(account);

        mockMvc.perform(delete("/api/1.0/upload/delete-profile")
                        .param("url", "https://storage.eka-dev.cloud/project/coffe/profiles/avatar.jpg")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /upload/upload-profile - Unauthorized (401)")
    void testUploadProfileUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "fake-image-content".getBytes());
        mockMvc.perform(multipart("/api/1.0/upload/upload-profile").file(file))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("DELETE /upload/delete-profile - Unauthorized (401)")
    void testDeleteProfileUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/1.0/upload/delete-profile").param("url", "https://storage.eka-dev.cloud/project/coffe/profiles/avatar.jpg"))
                .andExpect(status().is4xxClientError());
    }
}
