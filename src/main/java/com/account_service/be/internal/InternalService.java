package com.account_service.be.internal;

import com.account_service.be.account.AccountService;
import com.account_service.be.account.dto.NamesResponseDto;
import com.account_service.be.utils.commons.ResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class InternalService {

    private AccountService accountService;

    public InternalService(AccountService accountService) {
        this.accountService = accountService;
    }

    public ResponseEntity<ResponseModel<List<NamesResponseDto>>> getNameUsers(Integer[] userIdsArray) {
        List<NamesResponseDto> namesResponseDto = accountService.getNamesByUserIds(userIdsArray);
        ResponseModel<List<NamesResponseDto>> response = new ResponseModel<>(true, "Success Get Names", namesResponseDto);
        return ResponseEntity.ok(response);
    }

}
