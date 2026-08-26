package com.account_service.be.feature;

import com.account_service.be.utils.commons.BaseModal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tm_features")
public class FeatureModel extends BaseModal {
    @Id
    @Column(name = "feature_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int featureId;

    @Column(name = "feature_key", nullable = false, unique = true)
    private String featureKey;

    @Column(name = "feature_name", nullable = false)
    private String featureName;

    @Column(name = "description")
    private String description;
}
