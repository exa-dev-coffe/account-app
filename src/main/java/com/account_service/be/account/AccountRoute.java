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
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/1.0")
public class AccountRoute {
    private final AccountService accountService;

    public AccountRoute(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResponseModel<TokenResponseDto>> login(@RequestBody(required = false) LoginRequestDto loginRequest) {
        return accountService.login(loginRequest.getEmail(), loginRequest.getPassword());
    }

    @PostMapping("/auth/login-by-google")
    public ResponseEntity<ResponseModel<TokenResponseDto>> loginWithGoogle(@RequestBody LoginGoogleRequestDto loginRequest) throws Exception {
        return accountService.loginWithGoogle(loginRequest.getToken());
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
