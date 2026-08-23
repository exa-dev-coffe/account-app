package com.account_service.be.feature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<FeatureModel, Integer> {
    Optional<FeatureModel> findByFeatureKey(String featureKey);
}
