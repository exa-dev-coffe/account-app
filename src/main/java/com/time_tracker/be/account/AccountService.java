package com.time_tracker.be.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.time_tracker.be.account.dto.BaristaResponseDto;
import com.time_tracker.be.account.dto.CurrentUserDto;
import com.time_tracker.be.account.dto.MeResponseDto;
import com.time_tracker.be.account.dto.TokenResponseDto;
import com.time_tracker.be.account.projection.AccountProjection;
import com.time_tracker.be.account.projection.BaristaProjection;
import com.time_tracker.be.exception.BadRequestException;
import com.time_tracker.be.exception.NotFoundException;
import com.time_tracker.be.exception.TooManyRequestException;
import com.time_tracker.be.lib.RabbitmqService;
import com.time_tracker.be.refreshToken.RefreshTokenService;
import com.time_tracker.be.refreshToken.dto.AccountCacheDto;
import com.time_tracker.be.role.RoleModel;
import com.time_tracker.be.security.JwtTokenProvider;
import com.time_tracker.be.tokenResetPassword.ResetTokenPasswordService;
import com.time_tracker.be.utils.GoogleTokenUtils;
import com.time_tracker.be.utils.PasswordUtils;
import com.time_tracker.be.utils.commons.PaginationResponseDto;
import com.time_tracker.be.utils.commons.ResponseModel;
import com.time_tracker.be.utils.enums.TokenType;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;

@Slf4j
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final String CLIENT_ID;
    private final RabbitmqService rabbitmqService;
    private final String frontendUrl;
    private final ResetTokenPasswordService resetTokenPasswordService;

    public AccountService(AccountRepository accountRepository, JwtTokenProvider jwtTokenProvider, @Value("${spring.security.oauth2.authorizationserver.client.google.client-id}") String clientId, RefreshTokenService refreshTokenService, RabbitmqService rabbitmqService, @Value("${app.frontend.url}") String frontendUrl, ResetTokenPasswordService resetTokenPasswordService) {
        this.accountRepository = accountRepository;
        this.resetTokenPasswordService = resetTokenPasswordService;
        this.rabbitmqService = rabbitmqService;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.CLIENT_ID = clientId;
        this.frontendUrl = frontendUrl;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> login(String email, String password) {
        AccountModel user = this.accountRepository.findByEmail(email);

        if (user == null) {
            throw new BadRequestException("Password salah atau email tidak terdaftar");
        }

        boolean passwordMatch = PasswordUtils.matches(password, user.getPassword());

        if (!passwordMatch) {
            throw new BadRequestException("Password salah atau email tidak terdaftar");
        }

        TokenResponseDto data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days
        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Login berhasil", data);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> loginWithGoogle(String idTokenString) throws Exception {
        GoogleIdToken.Payload payload = GoogleTokenUtils.verifyGoogleToken(idTokenString);
        String email = (String) payload.get("email");
        boolean emailVerified = Boolean.parseBoolean((String) payload.get("email_verified"));
        String aud = (String) payload.get("aud");

        if (!emailVerified) {
            throw new BadRequestException("Email tidak terverifikasi");
        }

        if (!CLIENT_ID.equals(aud)) {
            throw new BadRequestException("Invalid audience");
        }

        AccountModel user = this.accountRepository.findByEmail(email);
        if (user == null) {
            throw new NotFoundException("Email tidak ditemukan");
        }

        TokenResponseDto data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days
        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Login berhasil", data);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> register(String email, String password, String name, Integer type) {
        AccountModel user = new AccountModel();
        RoleModel role = new RoleModel();
        role.setRoleId(type);
        user.setRole(role);
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(PasswordUtils.hashPassword(password));
        user.setPhoto(null);
        this.accountRepository.save(user);
        TokenResponseDto data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Registrasi berhasil", data);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token tidak ditemukan");
        }

        Claims claims = jwtTokenProvider.getClaims(refreshToken);

        boolean isExpired = claims.getExpiration().before(new Date());

        if (!claims.get("type").equals(TokenType.REFRESH.name())) {
            throw new BadRequestException("Refresh token tidak valid");
        }

        AccountModel user = this.accountRepository.findByUserId(Integer.parseInt(claims.get("userId").toString()));

        if (user == null) {
            throw new NotFoundException("User tidak ditemukan");
        }

        AccountCacheDto tokenFromDb = this.getTokenFromDb(refreshToken, user);
        if (tokenFromDb == null) {
            throw new BadRequestException("Refresh token tidak valid ");
        }

        if (!isExpired) {
            String newAccessToken = jwtTokenProvider.createToken(user, TokenType.ACCESS);
            TokenResponseDto data = new TokenResponseDto();
            data.setAccessToken(newAccessToken);
            data.setRefreshToken(refreshToken);
            ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Refresh token berhasil", data);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        } else {
            TokenResponseDto data = this.createTokenResponse(user);
            this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
            this.refreshTokenService.deleteRefreshToken(refreshToken, user);
            ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days

            ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Refresh token berhasil", data);
            return ResponseEntity.status(HttpStatus.OK)
                    .header("Set-Cookie", cookie.toString())
                    .body(response);
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<Object>> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token tidak ditemukan");
        }
        Claims claims = jwtTokenProvider.getClaims(refreshToken);
        AccountModel user = this.accountRepository.findByUserId(Integer.parseInt(claims.get("userId").toString()));
        this.refreshTokenService.deleteRefreshToken(refreshToken, user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", "", 0); // expire the cookie
        ResponseModel<Object> response = new ResponseModel<>(true, "Logout berhasil", null);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    public ResponseEntity<ResponseModel<MeResponseDto>> me(CurrentUserDto user) {
        if (user == null) {
            throw new NotFoundException("User tidak ditemukan");
        }
        AccountProjection data = this.accountRepository.findByUserId(user.getUserId(), AccountProjection.class);
        if (data == null) {
            throw new NotFoundException("User tidak ditemukan");
        }
        MeResponseDto me = new MeResponseDto();
        me.setUserId(data.getUserId());
        me.setEmail(data.getEmail());
        me.setFullName(data.getFullName());
        me.setPhoto(data.getPhoto());
        me.setRole(data.getRole().getRoleName());
        ResponseModel<MeResponseDto> response = new ResponseModel<>(true, "Data user ditemukan", me);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    public ResponseEntity<ResponseModel<String>> forgotPassword(String email) throws Exception {
        AccountModel user = this.accountRepository.findByEmail(email);

        if (user == null) {
            throw new NotFoundException("User tidak ditemukan");
        }

        if (this.resetTokenPasswordService.checkWasLimitOneDay(user)) {
            throw new TooManyRequestException("Anda telah mencapai batas maksimal permintaan reset password hari ini. Silakan coba lagi besok.");
        }

        String resetToken = jwtTokenProvider.createToken(user, TokenType.RESET_PASSWORD);
        this.resetTokenPasswordService.addResetToken(resetToken, user);
        HashMap<String, Object> emailPayload = new HashMap<>();
        emailPayload.put("to", user.getEmail());
        emailPayload.put("subject", "Reset Password");
        emailPayload.put("link", frontendUrl + "/reset-password?token=" + resetToken);
        ObjectMapper mapper = new ObjectMapper();
        String jsonMessage = mapper.writeValueAsString(emailPayload);

        this.rabbitmqService.sendMessage(
                "emailQueue.resetPassword",
                "",
                null,
                jsonMessage,
                true,
                false,
                false,
                null
        );


        ResponseModel<String> response = new ResponseModel<>(true, "Link reset password telah dikirim ke email Anda jika email terdaftar.", null);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> resetPassword(String token, String newPassword) throws Exception {
        Claims claims = jwtTokenProvider.getClaims(token);

        boolean isExpired = claims.getExpiration().before(new Date());

        if (isExpired) {
            throw new BadRequestException("Token expired");
        }

        if (!claims.get("type").equals(TokenType.RESET_PASSWORD.name())) {
            throw new BadRequestException("Token tidak valid");
        }

        AccountModel user = this.accountRepository.findByUserId(Integer.parseInt(claims.get("userId").toString()));

        if (user == null) {
            throw new NotFoundException("User tidak ditemukan");
        }

        user.setPassword(PasswordUtils.hashPassword(newPassword));
        this.accountRepository.save(user);

        this.resetTokenPasswordService.updateResetToken(token, user);

        HashMap<String, Object> emailPayload = new HashMap<>();
        emailPayload.put("to", user.getEmail());
        emailPayload.put("subject", "Reset Password Berhasil");
        ObjectMapper mapper = new ObjectMapper();
        String jsonMessage = mapper.writeValueAsString(emailPayload);

        this.rabbitmqService.sendMessage(
                "emailQueue.resetPasswordSuccess",
                "",
                null,
                jsonMessage,
                true,
                false,
                false,
                null
        );


        ResponseModel<String> response = new ResponseModel<>(true, "Password berhasil diubah", null);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    public ResponseEntity<ResponseModel<PaginationResponseDto<BaristaResponseDto>>> listBarista(Pageable pageable, String search) {
        RoleModel barista = new RoleModel();
        barista.setRoleId(3);
        Page<BaristaProjection> data = this.accountRepository.findByRoleAndEmailLikeIgnoreCase(barista, "%" + search + "%", pageable);
        Page<BaristaResponseDto> responseData = data.map(baristaData -> {
            BaristaResponseDto dto = new BaristaResponseDto();
            dto.setUserId(baristaData.getUserId());
            dto.setFullName(baristaData.getFullName());
            dto.setEmail(baristaData.getEmail());
            dto.setPhoto(baristaData.getPhoto());
            return dto;
        });
        PaginationResponseDto<BaristaResponseDto> responsePagination = new PaginationResponseDto<>();
        responsePagination.setData(responseData.getContent());
        responsePagination.setTotalData(responseData.getTotalElements());
        responsePagination.setTotalPages(responseData.getTotalPages());
        responsePagination.setCurrentPage(responseData.getNumber() + 1);
        responsePagination.setPageSize(responseData.getSize());
        responsePagination.setLastPage(responseData.isLast());

        ResponseModel<PaginationResponseDto<BaristaResponseDto>> response = new ResponseModel<>(true, "Data barista ditemukan", responsePagination);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);


    }

    private TokenResponseDto createTokenResponse(AccountModel user) {
        String accessToken = jwtTokenProvider.createToken(user, TokenType.ACCESS);
        String refreshToken = jwtTokenProvider.createToken(user, TokenType.REFRESH);
        TokenResponseDto data = new TokenResponseDto();
        data.setAccessToken(accessToken);
        data.setRefreshToken(refreshToken);
        return data;
    }

    private ResponseCookie createHttpOnlyCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }

    private AccountCacheDto getTokenFromDb(String token, AccountModel user) {
        return refreshTokenService.findByToken(token, user);
    }
}
