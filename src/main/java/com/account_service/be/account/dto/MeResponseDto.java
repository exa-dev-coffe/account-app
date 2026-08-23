package com.account_service.be.account.dto;

import com.account_service.be.roleFeature.dto.PermissionActionDto;
import lombok.Data;

import java.util.Map;

@Data
public class MeResponseDto {
    private Integer userId;
    private String email;
    private String fullName;
    private String role;
    private Integer roleId;
    private String photo;
    private Map<String, PermissionActionDto> permissions;
}
