package com.account_service.be.roleFeature.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRolePermissionRequestDto {
    @NotNull(message = "Permissions list is required")
    private List<RoleFeaturePermissionItemDto> permissions;
}
