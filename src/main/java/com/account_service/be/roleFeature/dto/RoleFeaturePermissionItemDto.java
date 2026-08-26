package com.account_service.be.roleFeature.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleFeaturePermissionItemDto {
    private int featureId;
    private String featureKey;
    private String featureName;
    private String description;

    @JsonAlias({"view", "can_view"})
    private boolean canView;

    @JsonAlias({"create", "can_create"})
    private boolean canCreate;

    @JsonAlias({"edit", "can_edit"})
    private boolean canEdit;

    @JsonAlias({"delete", "can_delete"})
    private boolean canDelete;

    private PermissionActionDto permissions;

    // Helper to get nested permissions object
    public PermissionActionDto getPermissions() {
        if (permissions != null) {
            return permissions;
        }
        return new PermissionActionDto(canView, canCreate, canEdit, canDelete);
    }
}
