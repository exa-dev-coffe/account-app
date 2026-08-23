package com.account_service.be.roleFeature;

import com.account_service.be.exception.BadRequestException;
import com.account_service.be.exception.NotFoundException;
import com.account_service.be.feature.FeatureModel;
import com.account_service.be.feature.FeatureRepository;
import com.account_service.be.feature.dto.FeatureResponseDto;
import com.account_service.be.lib.RabbitmqService;
import com.account_service.be.role.RoleModel;
import com.account_service.be.role.RoleRepository;
import com.account_service.be.roleFeature.dto.*;
import com.account_service.be.utils.enums.ExchangeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final FeatureRepository featureRepository;
    private final RoleFeatureRepository roleFeatureRepository;
    private final PermissionCacheService permissionCacheService;

    @Autowired(required = false)
    private RabbitmqService rabbitmqService;

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper();

    public RolePermissionService(
            RoleRepository roleRepository,
            FeatureRepository featureRepository,
            RoleFeatureRepository roleFeatureRepository,
            PermissionCacheService permissionCacheService
    ) {
        this.roleRepository = roleRepository;
        this.featureRepository = featureRepository;
        this.roleFeatureRepository = roleFeatureRepository;
        this.permissionCacheService = permissionCacheService;
    }

    public List<FeatureResponseDto> getAllFeatures() {
        return featureRepository.findAll().stream()
                .map(f -> FeatureResponseDto.builder()
                        .featureId(f.getFeatureId())
                        .featureKey(f.getFeatureKey())
                        .featureName(f.getFeatureName())
                        .description(f.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    public List<RolePermissionMatrixDto> getAllRolesWithPermissions() {
        List<RoleModel> roles = roleRepository.findAll();
        List<FeatureModel> allFeatures = featureRepository.findAll();

        return roles.stream()
                .filter(role -> role.getRoleId() != 2 && !"user".equalsIgnoreCase(role.getRoleName()) && !"customer".equalsIgnoreCase(role.getRoleName()))
                .map(role -> buildRolePermissionMatrix(role, allFeatures))
                .collect(Collectors.toList());
    }

    public RolePermissionMatrixDto getRolePermissions(int roleId) {
        RoleModel role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + roleId));
        List<FeatureModel> allFeatures = featureRepository.findAll();
        return buildRolePermissionMatrix(role, allFeatures);
    }

    @Transactional
    public RolePermissionMatrixDto updateRolePermissions(int roleId, UpdateRolePermissionRequestDto request, Integer currentUserId) {
        if (roleId == 1) {
            throw new BadRequestException("Super Admin permissions cannot be modified");
        }

        RoleModel role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + roleId));

        for (RoleFeaturePermissionItemDto item : request.getPermissions()) {
            FeatureModel feature = null;
            if (item.getFeatureId() > 0) {
                feature = featureRepository.findById(item.getFeatureId()).orElse(null);
            } else if (item.getFeatureKey() != null) {
                feature = featureRepository.findByFeatureKey(item.getFeatureKey()).orElse(null);
            }

            if (feature == null) {
                continue;
            }

            Optional<RoleFeatureModel> existingOpt = roleFeatureRepository.findByRole_RoleIdAndFeature_FeatureId(roleId, feature.getFeatureId());
            RoleFeatureModel rf;
            if (existingOpt.isPresent()) {
                rf = existingOpt.get();
            } else {
                rf = new RoleFeatureModel();
                rf.setRole(role);
                rf.setFeature(feature);
                rf.setCreatedBy(currentUserId);
            }

            rf.setCanView(item.isCanView());
            rf.setCanCreate(item.isCanCreate());
            rf.setCanEdit(item.isCanEdit());
            rf.setCanDelete(item.isCanDelete());
            rf.setUpdatedBy(currentUserId);

            roleFeatureRepository.save(rf);
        }

        // Synchronize Redis Cache immediately for live update across all microservices
        permissionCacheService.syncRolePermissionsToRedis(roleId);

        // Broadcast real-time permission update event to RabbitMQ (SSE Gateway)
        broadcastPermissionUpdateEvent(roleId, role.getRoleName());

        List<FeatureModel> allFeatures = featureRepository.findAll();
        return buildRolePermissionMatrix(role, allFeatures);
    }

    @Transactional
    public RolePermissionMatrixDto createRole(CreateRoleRequestDto request, Integer currentUserId) {
        if (roleRepository.findByRoleName(request.getRoleName().trim().toLowerCase()).isPresent()) {
            throw new BadRequestException("Role name already exists: " + request.getRoleName());
        }

        RoleModel newRole = new RoleModel();
        newRole.setRoleName(request.getRoleName().trim().toLowerCase());
        newRole.setCreatedBy(currentUserId);
        newRole.setUpdatedBy(currentUserId);
        newRole = roleRepository.save(newRole);

        if (request.getPermissions() != null) {
            for (RoleFeaturePermissionItemDto item : request.getPermissions()) {
                FeatureModel feature = null;
                if (item.getFeatureId() > 0) {
                    feature = featureRepository.findById(item.getFeatureId()).orElse(null);
                } else if (item.getFeatureKey() != null) {
                    feature = featureRepository.findByFeatureKey(item.getFeatureKey()).orElse(null);
                }

                if (feature != null) {
                    RoleFeatureModel rf = new RoleFeatureModel();
                    rf.setRole(newRole);
                    rf.setFeature(feature);
                    rf.setCanView(item.isCanView());
                    rf.setCanCreate(item.isCanCreate());
                    rf.setCanEdit(item.isCanEdit());
                    rf.setCanDelete(item.isCanDelete());
                    rf.setCreatedBy(currentUserId);
                    rf.setUpdatedBy(currentUserId);
                    roleFeatureRepository.save(rf);
                }
            }
        }

        permissionCacheService.syncRolePermissionsToRedis(newRole.getRoleId());
        broadcastPermissionUpdateEvent(newRole.getRoleId(), newRole.getRoleName());

        List<FeatureModel> allFeatures = featureRepository.findAll();
        return buildRolePermissionMatrix(newRole, allFeatures);
    }

    private void broadcastPermissionUpdateEvent(int roleId, String roleName) {
        if (rabbitmqService == null) {
            return;
        }

        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("event", "role_permission_updated");
            eventData.put("roleId", roleId);
            eventData.put("roleName", roleName);
            eventData.put("timestamp", Instant.now().toString());

            String jsonPayload = objectMapper.writeValueAsString(eventData);
            rabbitmqService.sendToExchange(
                    "role.permission.updated",
                    ExchangeType.FANOUT,
                    "",
                    jsonPayload,
                    false,
                    true,
                    null
            );
            log.info("Broadcasted role_permission_updated event for roleId: {} ({})", roleId, roleName);
        } catch (Exception e) {
            log.warn("Failed to broadcast role_permission_updated event: {}", e.getMessage());
        }
    }

    private RolePermissionMatrixDto buildRolePermissionMatrix(RoleModel role, List<FeatureModel> allFeatures) {
        boolean isAdmin = role.getRoleId() == 1 || "admin".equalsIgnoreCase(role.getRoleName());
        Map<String, PermissionActionDto> permissionsMap = new HashMap<>();
        List<RoleFeaturePermissionItemDto> featurePermissionsList = new ArrayList<>();

        if (isAdmin) {
            for (FeatureModel feature : allFeatures) {
                PermissionActionDto permDto = new PermissionActionDto(true, true, true, true);
                permissionsMap.put(feature.getFeatureKey().toLowerCase(), permDto);
                featurePermissionsList.add(RoleFeaturePermissionItemDto.builder()
                        .featureId(feature.getFeatureId())
                        .featureKey(feature.getFeatureKey())
                        .featureName(feature.getFeatureName())
                        .description(feature.getDescription())
                        .canView(true)
                        .canCreate(true)
                        .canEdit(true)
                        .canDelete(true)
                        .permissions(permDto)
                        .build());
            }
        } else {
            List<RoleFeatureModel> roleFeatures = roleFeatureRepository.findByRole_RoleId(role.getRoleId());
            Map<Integer, RoleFeatureModel> rfByFeatureId = roleFeatures.stream()
                    .collect(Collectors.toMap(rf -> rf.getFeature().getFeatureId(), rf -> rf, (a, b) -> a));

            for (FeatureModel feature : allFeatures) {
                RoleFeatureModel rf = rfByFeatureId.get(feature.getFeatureId());
                boolean v = rf != null && rf.isCanView();
                boolean c = rf != null && rf.isCanCreate();
                boolean e = rf != null && rf.isCanEdit();
                boolean d = rf != null && rf.isCanDelete();

                PermissionActionDto permDto = new PermissionActionDto(v, c, e, d);
                permissionsMap.put(feature.getFeatureKey().toLowerCase(), permDto);
                featurePermissionsList.add(RoleFeaturePermissionItemDto.builder()
                        .featureId(feature.getFeatureId())
                        .featureKey(feature.getFeatureKey())
                        .featureName(feature.getFeatureName())
                        .description(feature.getDescription())
                        .canView(v)
                        .canCreate(c)
                        .canEdit(e)
                        .canDelete(d)
                        .permissions(permDto)
                        .build());
            }
        }

        return RolePermissionMatrixDto.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .isLocked(isAdmin)
                .permissions(permissionsMap)
                .featurePermissions(featurePermissionsList)
                .build();
    }
}
