package com.account_service.be.account.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class NamesResponseDto {
    private Integer userId;
    private String fullName;
}
