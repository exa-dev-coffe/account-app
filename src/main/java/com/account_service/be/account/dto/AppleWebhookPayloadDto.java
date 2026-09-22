package com.account_service.be.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AppleWebhookPayloadDto {
    @JsonProperty("payload")
    private String payload;
}
