package com.account_service.be.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequestDto {
    @NotBlank(message = "Token cannot be empty")
    private String tokenTemp;

}
