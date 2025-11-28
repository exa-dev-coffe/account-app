package com.account_service.be.account;

import com.account_service.be.account.dto.*;
import com.account_service.be.annotation.CurrentUser;
import com.account_service.be.annotation.RequireAuth;
import com.account_service.be.annotation.RequireRole;
import com.account_service.be.utils.commons.CurrentUserDto;
import com.account_service.be.utils.commons.PaginationResponseDto;
import com.account_service.be.utils.commons.ResponseModel;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/1.0")
public class AccountRoute {
    private final AccountService accountService;
    @Value("${spring.security.oauth2.authorizationserver.client.google.client-id}")
    private String CLIENT_ID;
    @Value("${app.frontend.url}")
    private String FRONTEND_URL;
    @Value("${app.base-url}")
    private String BASE_URL;

    public AccountRoute(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResponseModel<TokenResponseDto>> login(@RequestBody(required = false) LoginRequestDto loginRequest) {
        return accountService.login(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @GetMapping("/auth/google/callback")
    public void loginGoogleCallback(HttpServletResponse response, @Param("code") String code) throws Exception {
        try {

            String redirectUri = BASE_URL + "/api/1.0/auth/google/callback";
            String res = accountService.loginGoogleCallback(code, redirectUri);
            response.sendRedirect(FRONTEND_URL + "/login?token_temp=" + res);
        } catch (Exception e) {
            String encodedError = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(FRONTEND_URL + "/login?error=" + encodedError);
        }
    }

    @PostMapping("/auth/google/login")
    public ResponseEntity<ResponseModel<TokenResponseDto>> loginGoogle(@RequestBody GoogleLoginRequestDto googleLoginRequest) throws Exception {
        return accountService.loginGoogle(googleLoginRequest.getTokenTemp());
    }

    @GetMapping("/auth/google")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {

        // Build redirect URI
        String redirectUri = BASE_URL + "/api/1.0/auth/google/callback";

        String oauthUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=openid email profile" +
                "&access_type=offline" +
                "&prompt=select_account";

        response.sendRedirect(oauthUrl);
    }


    @PostMapping("/auth/refresh")
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

    @PostMapping("/auth/logout")
    public ResponseEntity<ResponseModel<Object>> logout(@RequestBody(required = false) RefreshRequestDto refreshRequestDto, HttpServletRequest request) {
        String refreshToken = null;

        // ✅ Cek dari body kalau dikirim
        if (refreshRequestDto != null && refreshRequestDto.getRefreshToken() != null && !refreshRequestDto.getRefreshToken().trim().isEmpty()) {
            refreshToken = refreshRequestDto.getRefreshToken();
        } else {
            // ✅ Kalau body kosong, ambil dari cookie
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
        }
        return accountService.logout(refreshToken);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ResponseModel<TokenResponseDto>> register(@Valid @RequestBody RegisterRequestDto registerRequest) {
        return accountService.register(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getFullName(), null, 2);
    }

    @PostMapping("/barista/register-barista")
    @RequireRole({"admin"})
    public ResponseEntity<ResponseModel<TokenResponseDto>> registerBarista(@CurrentUser CurrentUserDto currentUser, @Valid @RequestBody RegisterRequestDto registerRequest) {
        return accountService.register(registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getFullName(), currentUser.getUserId(), 3);
    }

    @GetMapping("/me")
    @RequireAuth
    public ResponseEntity<ResponseModel<MeResponseDto>> me(@CurrentUser CurrentUserDto currentUser) {
        return accountService.me(currentUser);
    }

    @GetMapping("/barista/list-barista")
    @RequireRole({"admin"})
    public ResponseEntity<ResponseModel<PaginationResponseDto<BaristaResponseDto>>> listBarista(Pageable pageable, @Param("searchValue") String searchValue, @Param("searchKey") String searchKey) {
        // Kurangi 1, pastikan tidak negatif
        int pageNumber = pageable.getPageNumber() > 0 ? pageable.getPageNumber() - 1 : 0;

        // Buat Pageable baru
        Pageable adjustedPageable = PageRequest.of(pageNumber, pageable.getPageSize(), pageable.getSort());
        return accountService.listBarista(adjustedPageable, searchValue, searchKey);
    }

    @DeleteMapping("/barista")
    @RequireRole({"admin"})
    public ResponseEntity<ResponseModel<String>> deleteBarista(@RequestParam(name = "userId", required = false) Integer userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body(new ResponseModel<>(false, "UserId is required", null));
        }
        return accountService.deleteBarista(userId);
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<ResponseModel<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto forgotPasswordRequest) throws Exception {
        return accountService.forgotPassword(forgotPasswordRequest.getEmail());
    }

    @PostMapping("/auth/change-password")
    public ResponseEntity<ResponseModel<String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto resetPasswordRequest) throws Exception {
        return accountService.resetPassword(resetPasswordRequest.getToken(), resetPasswordRequest.getPassword());
    }

    @PatchMapping("/update-profile")
    @RequireAuth
    public ResponseEntity<ResponseModel<TokenResponseDto>> updateProfile(HttpServletRequest request, @CurrentUser CurrentUserDto currentUser, @Valid @RequestBody UpdateProfileRequestDto updateProfileRequest) {
        String refreshToken = null;

        // ✅ Cek dari body kalau dikirim
        if (updateProfileRequest.getRefreshToken() != null && !updateProfileRequest.getRefreshToken().trim().isEmpty()) {
            refreshToken = updateProfileRequest.getRefreshToken();
        } else {
            // ✅ Kalau body kosong, ambil dari cookie
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
        }
        return accountService.updateUser(refreshToken, currentUser.getUserId(), updateProfileRequest.getFullName(), updateProfileRequest.getPhoto());
    }


}
