package com.time_tracker.be.account;

import com.time_tracker.be.account.dto.LoginGoogleRequestDto;
import com.time_tracker.be.account.dto.LoginRequestDto;
import com.time_tracker.be.account.dto.RefreshRequestDto;
import com.time_tracker.be.account.dto.RegisterRequestDto;
import com.time_tracker.be.common.ResponseModel;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/1.0/auth")
public class AccountRoute {
    private final AccountService accountService;

    public AccountRoute(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseModel<Object>> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        return accountService.login(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @PostMapping("/login-by-google")
    public ResponseEntity<ResponseModel<Object>> loginWithGoogle(@RequestBody LoginGoogleRequestDto loginRequest) throws Exception {
        return accountService.loginWithGoogle(loginRequest.getToken());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseModel<Object>> refreshToken(@RequestBody RefreshRequestDto refreshRequestDto, HttpServletRequest request) {
        String refreshToken = refreshRequestDto.getRefreshToken();

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("refreshToken")) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
        }
        return accountService.refreshToken(refreshToken);
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseModel<Object>> register(@Valid @RequestBody RegisterRequestDto registerRequest) {
        return accountService.register(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getFullName());
    }

}
