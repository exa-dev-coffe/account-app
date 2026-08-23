package com.account_service.be.roleFeature.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRoleRequestDto {
    @NotBlank(message = "Role name is required")
    private String roleName;

    private List<RoleFeaturePermissionItemDto> permissions;
}
