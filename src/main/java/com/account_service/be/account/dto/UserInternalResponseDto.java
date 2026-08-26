package com.account_service.be.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInternalResponseDto {
    private int userId;
    private String email;
    private String fullName;
    private String roleName;
}
