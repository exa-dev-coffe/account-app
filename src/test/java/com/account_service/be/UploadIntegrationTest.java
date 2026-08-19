package com.account_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Profile Avatar Upload Endpoints")
class UploadIntegrationTest extends BaseIntegrationTest {

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
        mockMvc.perform(delete("/api/1.0/upload/delete-profile").param("url", "profile.jpg"))
                .andExpect(status().is4xxClientError());
    }
}
