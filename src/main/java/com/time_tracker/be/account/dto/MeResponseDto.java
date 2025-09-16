package com.time_tracker.be.account.dto;

import lombok.Data;

@Data
public class MeResponseDto {
    private Integer userId;
    private String email;
    private String fullName;
    private String role;
    private String photo;
}
