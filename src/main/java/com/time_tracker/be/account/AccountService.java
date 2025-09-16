package com.time_tracker.be.account;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.time_tracker.be.common.ResponseModel;
import com.time_tracker.be.common.TokenType;
import com.time_tracker.be.exception.BadRequestException;
import com.time_tracker.be.exception.NotFoundException;
import com.time_tracker.be.refreshToken.RefreshTokenService;
import com.time_tracker.be.refreshToken.dto.AccountCacheDto;
import com.time_tracker.be.role.RoleModel;
import com.time_tracker.be.security.JwtTokenProvider;
import com.time_tracker.be.utils.GoogleTokenUtils;
import com.time_tracker.be.utils.PasswordUtils;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final String CLIENT_ID;

    public AccountService(AccountRepository accountRepository, JwtTokenProvider jwtTokenProvider, @Value("${spring.security.oauth2.authorizationserver.client.google.client-id}") String clientId, RefreshTokenService refreshTokenService) {
        this.accountRepository = accountRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenProvider = jwtTokenProvider;
        CLIENT_ID = clientId;
    }

    @Transactional(Transactional.TxType.REQUIRED)

    public ResponseEntity<ResponseModel<Object>> login(String email, String password) {
        AccountModel user = this.accountRepository.findByEmail(email);

        if (user == null) {
            throw new BadRequestException("Password salah atau email tidak terdaftar");
        }

        boolean passwordMatch = PasswordUtils.matches(password, user.getPassword());

        if (!passwordMatch) {
            throw new BadRequestException("Password salah atau email tidak terdaftar");
        }

        HashMap<String, Object> data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.get("refreshToken").toString(), user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.get("refreshToken").toString(), 7 * 24 * 60 * 60); // 7 days
        ResponseModel<Object> response = new ResponseModel<>(true, "Login berhasil", data);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<Object>> loginWithGoogle(String idTokenString) throws Exception {
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

        HashMap<String, Object> data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.get("refreshToken").toString(), user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.get("refreshToken").toString(), 7 * 24 * 60 * 60); // 7 days
        ResponseModel<Object> response = new ResponseModel<>(true, "Login berhasil", data);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<Object>> register(String email, String password, String name) {
        AccountModel user = new AccountModel();
        RoleModel role = new RoleModel();
        role.setRoleId(2); // role_id 2 adalah user)
        user.setRoleId(role);
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(PasswordUtils.hashPassword(password));
        user.setRoleId(role);
        user.setPhoto(null);
        user.setBalanceId(null); // balance di-set null, karena akan di-set setelah registrasi di BalanceService
        this.accountRepository.save(user);
        HashMap<String, Object> data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.get("refreshToken").toString(), user);
        ResponseModel<Object> response = new ResponseModel<>(true, "Registrasi berhasil", data);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.get("refreshToken").toString(), 7 * 24 * 60 * 60); // 7 days
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    public ResponseEntity<ResponseModel<Object>> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token tidak ditemukan");
        }

        boolean isExpired = jwtTokenProvider.validateToken(refreshToken);


        Claims claims = jwtTokenProvider.getClaims(refreshToken);

        AccountModel user = this.accountRepository.findByEmailAndUserId(claims.get("email").toString(), Integer.parseInt(claims.get("userId").toString()));

        if (user == null) {
            throw new NotFoundException("User tidak ditemukan");
        }

        AccountCacheDto tokenFromDb = this.getTokenFromDb(refreshToken, user);
        if (tokenFromDb == null) {
            throw new BadRequestException("Refresh token tidak valid atau sudah kadaluarsa");
        }

        if (!isExpired) {
            String newAccessToken = jwtTokenProvider.createToken(user, TokenType.ACCESS);
            HashMap<String, Object> data = new HashMap<>();
            data.put("accessToken", newAccessToken);
            data.put("refreshToken", refreshToken);
            ResponseModel<Object> response = new ResponseModel<>(true, "Refresh token berhasil", data);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        } else {
            HashMap<String, Object> data = this.createTokenResponse(user);
            this.refreshTokenService.addRefreshToken(data.get("refreshToken").toString(), user);
            this.refreshTokenService.deleteRefreshToken(refreshToken, user);
            ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.get("refreshToken").toString(), 7 * 24 * 60 * 60); // 7 days
            ResponseModel<Object> response = new ResponseModel<>(true, "Refresh token berhasil", data);
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
        AccountModel user = this.accountRepository.findByEmailAndUserId(claims.get("email").toString(), Integer.parseInt(claims.get("userId").toString()));
        this.refreshTokenService.deleteRefreshToken(refreshToken, user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", "", 0); // expire the cookie
        ResponseModel<Object> response = new ResponseModel<>(true, "Logout berhasil", null);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    private HashMap<String, Object> createTokenResponse(AccountModel user) {
        String accessToken = jwtTokenProvider.createToken(user, TokenType.ACCESS);
        String refreshToken = jwtTokenProvider.createToken(user, TokenType.REFRESH);
        HashMap<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        return data;
    }

    private ResponseCookie createHttpOnlyCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite("None")
                .build();
    }

    private AccountCacheDto getTokenFromDb(String token, AccountModel user) {
        return refreshTokenService.findByTokenAndUserId(token, user);
    }
}
