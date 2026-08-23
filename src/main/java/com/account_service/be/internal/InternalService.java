package com.account_service.be.internal;

import com.account_service.be.account.AccountService;
import com.account_service.be.account.dto.NamesResponseDto;
import com.account_service.be.roleFeature.PermissionCacheService;
import com.account_service.be.roleFeature.dto.PermissionActionDto;
import com.account_service.be.utils.commons.ResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class InternalService {

    private final AccountService accountService;
    private final PermissionCacheService permissionCacheService;

    public InternalService(AccountService accountService, PermissionCacheService permissionCacheService) {
        this.accountService = accountService;
        this.permissionCacheService = permissionCacheService;
    }

    public ResponseEntity<ResponseModel<List<NamesResponseDto>>> getNameUsers(Integer[] userIdsArray) {
        List<NamesResponseDto> namesResponseDto = accountService.getNamesByUserIds(userIdsArray);
        ResponseModel<List<NamesResponseDto>> response = new ResponseModel<>(true, "Success Get Names", namesResponseDto);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResponseModel<Map<String, PermissionActionDto>>> getRolePermissions(int roleId) {
        Map<String, PermissionActionDto> permissions = permissionCacheService.getRolePermissions(roleId);
        ResponseModel<Map<String, PermissionActionDto>> response = new ResponseModel<>(true, "Success Get Role Permissions", permissions);
        return ResponseEntity.ok(response);
    }
}
