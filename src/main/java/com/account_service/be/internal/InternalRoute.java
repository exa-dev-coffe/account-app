package com.account_service.be.internal;

import com.account_service.be.account.dto.NamesResponseDto;
import com.account_service.be.annotation.ValidateSignature;
import com.account_service.be.exception.BadRequestException;
import com.account_service.be.roleFeature.dto.PermissionActionDto;
import com.account_service.be.utils.commons.ResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/internal")
public class InternalRoute {
    private final InternalService internalService;

    public InternalRoute(InternalService internalService) {
        this.internalService = internalService;
    }

    @GetMapping("/name-users")
    @ValidateSignature
    public ResponseEntity<ResponseModel<List<NamesResponseDto>>> getNameUsers(@RequestParam("ids") String ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("User IDs parameter is required");
        }
        Integer[] userIdsArray = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .parallel()
                .toArray(Integer[]::new);
        return internalService.getNameUsers(userIdsArray);
    }

    @GetMapping("/roles/{roleId}/permissions")
    @ValidateSignature
    public ResponseEntity<ResponseModel<Map<String, PermissionActionDto>>> getRolePermissions(@PathVariable("roleId") int roleId) {
        return internalService.getRolePermissions(roleId);
    }
}
