package com.account_service.be.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NamesResponseDto {
    private Integer userId;
    private String fullName;
    private String email;
}
