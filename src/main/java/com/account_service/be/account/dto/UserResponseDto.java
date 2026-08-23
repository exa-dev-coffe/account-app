package com.account_service.be.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private int userId;
    private String email;
    private String fullName;
    private String photo;
    private int roleId;
    private String roleName;
    private Date createdAt;
    private Date updatedAt;
}
