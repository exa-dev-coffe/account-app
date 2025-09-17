package com.time_tracker.be.account;

import com.time_tracker.be.account.dto.*;
import com.time_tracker.be.annotation.CurrentUser;
import com.time_tracker.be.common.ResponseModel;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/1.0/auth")
public class AccountRoute {
    private final AccountService accountService;

    public AccountRoute(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseModel<TokenResponseDto>> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        return accountService.login(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @PostMapping("/login-by-google")
    public ResponseEntity<ResponseModel<TokenResponseDto>> loginWithGoogle(@RequestBody LoginGoogleRequestDto loginRequest) throws Exception {
        return accountService.loginWithGoogle(loginRequest.getToken());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseModel<TokenResponseDto>> refreshToken(@RequestBody(required = false) RefreshRequestDto refreshRequestDto, HttpServletRequest request) {
        String refreshToken = null;
        if (refreshRequestDto != null) {
            refreshToken = refreshRequestDto.getRefreshToken();
        }

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

    @PostMapping("/logout")
    public ResponseEntity<ResponseModel<Object>> logout(@RequestBody RefreshRequestDto refreshRequestDto, HttpServletRequest request) {
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
        return accountService.logout(refreshToken);
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseModel<TokenResponseDto>> register(@Valid @RequestBody RegisterRequestDto registerRequest) {
        return accountService.register(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getFullName(), 2);
    }

    @PostMapping("/register-barista")
    public ResponseEntity<ResponseModel<TokenResponseDto>> registerBarista(@Valid @RequestBody RegisterRequestDto registerRequest) {
        return accountService.register(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getFullName(), 3);
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseModel<MeResponseDto>> me(@CurrentUser CurrentUserDto currentUser) {
        return accountService.me(currentUser);
    }

}
