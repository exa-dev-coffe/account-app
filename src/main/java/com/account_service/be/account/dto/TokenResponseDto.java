package com.account_service.be.account.dto;

import lombok.Data;

@Data
public class TokenResponseDto {
    private String accessToken;
    private String refreshToken;
}
