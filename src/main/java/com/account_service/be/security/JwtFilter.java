package com.account_service.be.security;

import com.account_service.be.annotation.ActionType;
import com.account_service.be.annotation.RequireAuth;
import com.account_service.be.annotation.RequirePermission;
import com.account_service.be.annotation.RequireRole;
import com.account_service.be.exception.ForbiddenException;
import com.account_service.be.exception.NotAuthorizedException;
import com.account_service.be.lib.JwtService;
import com.account_service.be.roleFeature.PermissionCacheService;
import com.account_service.be.roleFeature.dto.PermissionActionDto;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Aspect
@Component
public class JwtFilter {
    private final JwtService jwtService;
    private final HttpServletRequest request;
    private final PermissionCacheService permissionCacheService;

    public JwtFilter(JwtService jwtService, HttpServletRequest request, PermissionCacheService permissionCacheService) {
        this.jwtService = jwtService;
        this.request = request;
        this.permissionCacheService = permissionCacheService;
    }

    @Around("@annotation(requireAuth)")
    public Object checkAuth(ProceedingJoinPoint pjp, RequireAuth requireAuth) throws Throwable {
        String header = request.getHeader("Authorization");
        String token = jwtService.resolveToken(header);

        if (token == null || jwtService.getClaims(token).getExpiration().before(new Date())) {
            throw new NotAuthorizedException("Token is not valid");
        }

        return pjp.proceed();
    }

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequireRole requireRole) throws Throwable {
        String header = request.getHeader("Authorization");
        String token = jwtService.resolveToken(header);

        if (token == null || jwtService.getClaims(token).getExpiration().before(new Date())) {
            throw new NotAuthorizedException("Token is not valid");
        }

        String role = (String) jwtService.getClaims(token).get("role");
        boolean hasRole = false;
        for (String r : requireRole.value()) {
            if (r.equalsIgnoreCase(role)) {
                hasRole = true;
                break;
            }
        }
        if (!hasRole) {
            throw new ForbiddenException("You don't have permission to access this resource");
        }

        return pjp.proceed();
    }

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequirePermission requirePermission) throws Throwable {
        String header = request.getHeader("Authorization");
        String token = jwtService.resolveToken(header);

        if (token == null) {
            throw new NotAuthorizedException("Token is missing");
        }

        Claims claims = jwtService.getClaims(token);
        if (claims.getExpiration().before(new Date())) {
            throw new NotAuthorizedException("Token is expired");
        }

        String role = (String) claims.get("role");
        Integer roleId = claims.get("roleId", Integer.class);

        // Super Admin always has full access
        if ("admin".equalsIgnoreCase(role) || (roleId != null && roleId == 1)) {
            return pjp.proceed();
        }

        if (roleId == null) {
            throw new ForbiddenException("Role information missing in token");
        }

        Map<String, PermissionActionDto> permissions = permissionCacheService.getRolePermissions(roleId);
        String featureKey = requirePermission.feature().toLowerCase();
        PermissionActionDto actionPerm = permissions.get(featureKey);

        if (actionPerm == null) {
            throw new ForbiddenException("You don't have permission to access " + requirePermission.feature());
        }

        boolean allowed = switch (requirePermission.action()) {
            case VIEW -> actionPerm.isView();
            case CREATE -> actionPerm.isCreate();
            case EDIT -> actionPerm.isEdit();
            case DELETE -> actionPerm.isDelete();
        };

        if (!allowed) {
            throw new ForbiddenException("You don't have permission to " + requirePermission.action().name().toLowerCase() + " " + requirePermission.feature());
        }

        return pjp.proceed();
    }
}