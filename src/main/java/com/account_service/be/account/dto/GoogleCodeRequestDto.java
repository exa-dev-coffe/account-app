package com.account_service.be.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleCodeRequestDto {

    @NotBlank(message = "OAuth Authorization code is required")
    private String code;

}
