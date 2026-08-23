package com.account_service.be.roleFeature.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionMatrixDto {
    private int roleId;
    private String roleName;
    private boolean isLocked; // true for Admin (full access, non-editable)
    private Map<String, PermissionActionDto> permissions;

    @JsonAlias({"features"})
    private List<RoleFeaturePermissionItemDto> featurePermissions;

    public List<RoleFeaturePermissionItemDto> getFeatures() {
        return featurePermissions;
    }
}
