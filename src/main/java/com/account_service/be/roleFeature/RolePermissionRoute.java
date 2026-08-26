package com.account_service.be.roleFeature;

import com.account_service.be.annotation.CurrentUser;
import com.account_service.be.annotation.RequireRole;
import com.account_service.be.feature.dto.FeatureResponseDto;
import com.account_service.be.roleFeature.dto.CreateRoleRequestDto;
import com.account_service.be.roleFeature.dto.RolePermissionMatrixDto;
import com.account_service.be.roleFeature.dto.UpdateRolePermissionRequestDto;
import com.account_service.be.utils.commons.CurrentUserDto;
import com.account_service.be.utils.commons.ResponseModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/1.0/admin")
public class RolePermissionRoute {

    private final RolePermissionService rolePermissionService;

    public RolePermissionRoute(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping("/features")
    @RequireRole({"admin"})
    public ResponseEntity<ResponseModel<List<FeatureResponseDto>>> getAllFeatures() {
        List<FeatureResponseDto> data = rolePermissionService.getAllFeatures();
        return ResponseEntity.ok(new ResponseModel<>(true, "Features retrieved successfully", data));
    }

    @GetMapping("/roles")
    @RequireRole({"admin"})
    public ResponseEntity<ResponseModel<List<RolePermissionMatrixDto>>> getAllRoles() {
        List<RolePermissionMatrixDto> data = rolePermissionService.getAllRolesWithPermissions();
        return ResponseEntity.ok(new ResponseModel<>(true, "Roles retrieved successfully", data));
    }

    @GetMapping("/roles/{roleId}/permissions")
    @RequireRole({"admin"})
    public ResponseEntity<ResponseModel<RolePermissionMatrixDto>> getRolePermissions(@PathVariable("roleId") int roleId) {
        RolePermissionMatrixDto data = rolePermissionService.getRolePermissions(roleId);
        return ResponseEntity.ok(new ResponseModel<>(true, "Role permissions retrieved successfully", data));
    }

    @PutMapping("/roles/{roleId}/permissions")
    @RequireRole({"admin"})
    public ResponseEntity<ResponseModel<RolePermissionMatrixDto>> updateRolePermissions(
            @PathVariable("roleId") int roleId,
            @Valid @RequestBody UpdateRolePermissionRequestDto request,
            @CurrentUser CurrentUserDto currentUser
    ) {
        Integer currentUserId = currentUser != null ? currentUser.getUserId() : null;
        RolePermissionMatrixDto data = rolePermissionService.updateRolePermissions(roleId, request, currentUserId);
        return ResponseEntity.ok(new ResponseModel<>(true, "Role permissions updated successfully", data));
    }

    @PostMapping("/roles")
    @RequireRole({"admin"})
    public ResponseEntity<ResponseModel<RolePermissionMatrixDto>> createRole(
            @Valid @RequestBody CreateRoleRequestDto request,
            @CurrentUser CurrentUserDto currentUser
    ) {
        Integer currentUserId = currentUser != null ? currentUser.getUserId() : null;
        RolePermissionMatrixDto data = rolePermissionService.createRole(request, currentUserId);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(new ResponseModel<>(true, "Role created successfully", data));
    }
}
