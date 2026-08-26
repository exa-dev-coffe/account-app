package com.account_service.be.roleFeature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleFeatureRepository extends JpaRepository<RoleFeatureModel, Integer> {
    List<RoleFeatureModel> findByRole_RoleId(int roleId);
    Optional<RoleFeatureModel> findByRole_RoleIdAndFeature_FeatureKey(int roleId, String featureKey);
    Optional<RoleFeatureModel> findByRole_RoleIdAndFeature_FeatureId(int roleId, int featureId);
    void deleteByRole_RoleId(int roleId);
}
