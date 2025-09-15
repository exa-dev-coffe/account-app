package com.time_tracker.be.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginGoogleRequestDto {

    @NotBlank(message = "Token cannot be empty")
    private String token;
}
