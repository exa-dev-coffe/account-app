package com.account_service.be.refreshToken.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountCacheDto {
    private Integer id;
    private String email;
    private String fullName;
    private String role;
}
