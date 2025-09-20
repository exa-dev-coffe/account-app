package com.time_tracker.be.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDto {
    @NotBlank(message = "Token cannot be empty")
    private String token;
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must be at least 6 characters long")
    private String password;
}
