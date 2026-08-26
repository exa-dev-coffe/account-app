package com.account_service.be.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserAdminRequestDto {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Role ID is required")
    private Integer roleId;

    private String photo;
}
