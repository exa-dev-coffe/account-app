package com.time_tracker.be.account.dto;

import lombok.Data;

@Data
public class CurrentUserDto {
    private String email;
    private String fullName;
    private String token;
    private Integer userId;
    private String role;

}
