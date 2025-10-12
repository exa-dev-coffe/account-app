package com.account_service.be.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password should be at least 8 characters long")
    @Size(max = 20, message = "Password should not exceed 20 characters")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(min = 3, message = "Name should be at least 3 characters long")
    private String fullName;

}
