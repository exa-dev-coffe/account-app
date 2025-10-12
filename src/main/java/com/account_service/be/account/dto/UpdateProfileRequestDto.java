package com.account_service.be.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequestDto {
    @NotBlank(message = "Name is required")
    private String fullName;
    @NotBlank(message = "Photo is required")
    private String photo;
    private String refreshToken;
}
