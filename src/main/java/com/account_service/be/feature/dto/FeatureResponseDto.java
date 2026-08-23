package com.account_service.be.feature.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeatureResponseDto {
    private int featureId;
    private String featureKey;
    private String featureName;
    private String description;
}
