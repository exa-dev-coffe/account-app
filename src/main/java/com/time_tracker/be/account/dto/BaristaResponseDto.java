package com.time_tracker.be.account.dto;

import lombok.Data;

@Data
public class BaristaResponseDto {
    int userId;
    String email;
    String fullName;
    String photo;
}
