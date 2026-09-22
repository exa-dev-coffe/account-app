package com.account_service.be.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppleAuthRequestDto {
    @NotBlank(message = "Identity token or authorization code is required")
    private String identityToken;

    private String code;
    private String firstName;
    private String lastName;
    private String user; // Raw Apple user object JSON if sent by Apple JS SDK
}
