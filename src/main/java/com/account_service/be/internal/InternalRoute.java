package com.account_service.be.internal;

import com.account_service.be.account.dto.NamesResponseDto;
import com.account_service.be.annotation.ValidateSignature;
import com.account_service.be.exception.BadRequestException;
import com.account_service.be.utils.commons.ResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

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
}
