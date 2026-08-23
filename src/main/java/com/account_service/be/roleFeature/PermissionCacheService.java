package com.account_service.be.roleFeature;

import com.account_service.be.feature.FeatureModel;
import com.account_service.be.feature.FeatureRepository;
import com.account_service.be.roleFeature.dto.PermissionActionDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PermissionCacheService {

    private static final String REDIS_KEY_PREFIX = "auth:role_permissions:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoleFeatureRepository roleFeatureRepository;
    private final FeatureRepository featureRepository;
    private final ObjectMapper objectMapper;

    public PermissionCacheService(
            RedisTemplate<String, Object> redisTemplate,
            RoleFeatureRepository roleFeatureRepository,
            FeatureRepository featureRepository,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.roleFeatureRepository = roleFeatureRepository;
        this.featureRepository = featureRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, PermissionActionDto> getRolePermissions(int roleId) {
        String key = REDIS_KEY_PREFIX + roleId;

        // 1. Try reading from Redis
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                String jsonString = cached instanceof String ? (String) cached : objectMapper.writeValueAsString(cached);
                return objectMapper.readValue(jsonString, new TypeReference<Map<String, PermissionActionDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to fetch role permissions from Redis for roleId={}: {}", roleId, e.getMessage());
        }

        // 2. Cache miss: Fetch from PostgreSQL database
        Map<String, PermissionActionDto> permissions = loadPermissionsFromDb(roleId);

        // 3. Warm Redis cache
        syncRolePermissionsToRedis(roleId, permissions);

        return permissions;
    }

    public Map<String, PermissionActionDto> loadPermissionsFromDb(int roleId) {
        Map<String, PermissionActionDto> permissionsMap = new HashMap<>();

        // If roleId == 1 (admin), provide full permissions for all active features
        if (roleId == 1) {
            List<FeatureModel> allFeatures = featureRepository.findAll();
            for (FeatureModel feature : allFeatures) {
                permissionsMap.put(feature.getFeatureKey().toLowerCase(),
                        new PermissionActionDto(true, true, true, true));
            }
            return permissionsMap;
        }

        List<RoleFeatureModel> roleFeatures = roleFeatureRepository.findByRole_RoleId(roleId);
        for (RoleFeatureModel rf : roleFeatures) {
            if (rf.getFeature() != null && rf.getFeature().getFeatureKey() != null) {
                permissionsMap.put(
                        rf.getFeature().getFeatureKey().toLowerCase(),
                        new PermissionActionDto(rf.isCanView(), rf.isCanCreate(), rf.isCanEdit(), rf.isCanDelete())
                );
            }
        }

        return permissionsMap;
    }

    public void syncRolePermissionsToRedis(int roleId, Map<String, PermissionActionDto> permissions) {
        String key = REDIS_KEY_PREFIX + roleId;
        try {
            String json = objectMapper.writeValueAsString(permissions);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            log.info("Synchronized role permissions to Redis for roleId={}", roleId);
        } catch (Exception e) {
            log.error("Failed to sync role permissions to Redis for roleId={}: {}", roleId, e.getMessage());
        }
    }

    public void syncRolePermissionsToRedis(int roleId) {
        Map<String, PermissionActionDto> permissions = loadPermissionsFromDb(roleId);
        syncRolePermissionsToRedis(roleId, permissions);
    }

    public void invalidateRolePermissions(int roleId) {
        String key = REDIS_KEY_PREFIX + roleId;
        try {
            redisTemplate.delete(key);
            log.info("Invalidated Redis cache for roleId={}", roleId);
        } catch (Exception e) {
            log.error("Failed to invalidate Redis cache for roleId={}: {}", roleId, e.getMessage());
        }
    }
}
