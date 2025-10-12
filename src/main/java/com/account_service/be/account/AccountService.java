package com.account_service.be.account;

import com.account_service.be.account.dto.BaristaResponseDto;
import com.account_service.be.account.dto.MeResponseDto;
import com.account_service.be.account.dto.TokenResponseDto;
import com.account_service.be.account.projection.AccountProjection;
import com.account_service.be.exception.BadRequestException;
import com.account_service.be.exception.NotFoundException;
import com.account_service.be.exception.TooManyRequestException;
import com.account_service.be.lib.JwtService;
import com.account_service.be.lib.RabbitmqService;
import com.account_service.be.refreshToken.RefreshTokenService;
import com.account_service.be.refreshToken.dto.AccountCacheDto;
import com.account_service.be.role.RoleModel;
import com.account_service.be.tokenResetPassword.ResetTokenPasswordService;
import com.account_service.be.utils.GoogleTokenUtils;
import com.account_service.be.utils.PasswordUtils;
import com.account_service.be.utils.commons.CurrentUserDto;
import com.account_service.be.utils.commons.GenericSpecification;
import com.account_service.be.utils.commons.PaginationResponseDto;
import com.account_service.be.utils.commons.ResponseModel;
import com.account_service.be.utils.enums.ExchangeType;
import com.account_service.be.utils.enums.TokenType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.jsonwebtoken.Claims;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final String CLIENT_ID;
    private final RabbitmqService rabbitmqService;
    private final String frontendUrl;
    private final ResetTokenPasswordService resetTokenPasswordService;

    public AccountService(AccountRepository accountRepository, JwtService jwtService, @Value("${spring.security.oauth2.authorizationserver.client.google.client-id}") String clientId, RefreshTokenService refreshTokenService, RabbitmqService rabbitmqService, @Value("${app.frontend.url}") String frontendUrl, ResetTokenPasswordService resetTokenPasswordService) {
        this.accountRepository = accountRepository;
        this.resetTokenPasswordService = resetTokenPasswordService;
        this.rabbitmqService = rabbitmqService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
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
    public ResponseEntity<ResponseModel<TokenResponseDto>> register(String email, String password, String name, Integer userId, Integer type) {
        AccountModel user = new AccountModel();
        RoleModel role = new RoleModel();
        role.setRoleId(type);
        user.setRole(role);
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(PasswordUtils.hashPassword(password));
        user.setPhoto(null);
        user.setCreatedBy(userId);
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

        Claims claims = jwtService.getClaims(refreshToken);

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
            String newAccessToken = jwtService.createToken(user, TokenType.ACCESS);
            TokenResponseDto data = new TokenResponseDto();
            data.setAccessToken(newAccessToken);
            data.setRefreshToken(refreshToken);
            ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Refresh token berhasil", data);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        } else {
            TokenResponseDto data = this.createTokenResponse(user);
            this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
            this.refreshTokenService.deleteRefreshTokenByToken(refreshToken);
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
        this.refreshTokenService.deleteRefreshTokenByToken(refreshToken);
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

        String resetToken = jwtService.createToken(user, TokenType.RESET_PASSWORD);
        this.resetTokenPasswordService.addResetToken(resetToken, user);
        HashMap<String, Object> emailPayload = new HashMap<>();
        emailPayload.put("to", user.getEmail());
        emailPayload.put("subject", "Reset Password");
        emailPayload.put("link", frontendUrl + "/reset-password?token=" + resetToken);
        ObjectMapper mapper = new ObjectMapper();
        String jsonMessage = mapper.writeValueAsString(emailPayload);

        this.rabbitmqService.sendMessage(
                "Email Reset Password",
                "emailQueue.resetPassword",
                "",
                ExchangeType.DIRECT,
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
        Claims claims = jwtService.getClaims(token);

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
                "Email Reset Password Success",
                "emailQueue.resetPasswordSuccess",
                "",
                ExchangeType.DIRECT,
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

    public ResponseEntity<ResponseModel<PaginationResponseDto<BaristaResponseDto>>> listBarista(Pageable pageable, String searchValue, String searchKey) {
        RoleModel barista = new RoleModel();
        barista.setRoleId(3);
        Specification<AccountModel> spec = Specification
                .where((root, query, cb) -> {
                    // Predicate untuk role
                    Predicate rolePredicate = cb.equal(root.get("role").get("roleId"), barista.getRoleId());

                    // Predicate untuk dynamic filter (dari GenericSpecification)
                    Predicate dynamicPredicate = GenericSpecification.<AccountModel>dynamicFilter(searchKey, searchValue)
                            .toPredicate(root, query, cb);

                    // Gabung dengan OR atau AND sesuai kebutuhan
                    return cb.and(rolePredicate, dynamicPredicate); // pakai AND
                });

        Page<AccountModel> data = accountRepository.findAll(spec, pageable);
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

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> deleteBarista(Integer baristaId) {
        AccountModel user = this.accountRepository.findById(baristaId).orElse(null);

        if (user == null) {
            throw new NotFoundException("User tidak ditemukan");
        }

        if (user.getRole().getRoleId() != 3) {
            throw new BadRequestException("User bukan barista");
        }

        this.refreshTokenService.deleteRefreshTokenByUser(user);
        this.accountRepository.delete(user);

        ResponseModel<String> response = new ResponseModel<>(true, "Barista berhasil dihapus", null);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> updateUser(String refreshToken, Integer userId, String fullName, String photo) {
        AccountModel user = this.accountRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new NotFoundException("User tidak ditemukan");
        }

        user.setFullName(fullName);
        user.setPhoto(photo);
        user.setUpdatedAt(new Date());
        user.setUpdatedBy(userId);
        this.accountRepository.save(user);

        TokenResponseDto data = this.createTokenResponse(user);
        this.refreshTokenService.deleteRefreshTokenByToken(refreshToken);
        this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days

        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "User berhasil diupdate", data);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    private TokenResponseDto createTokenResponse(AccountModel user) {
        String accessToken = jwtService.createToken(user, TokenType.ACCESS);
        String refreshToken = jwtService.createToken(user, TokenType.REFRESH);
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
                .sameSite("None")
                .build();
    }

    private AccountCacheDto getTokenFromDb(String token, AccountModel user) {
        return refreshTokenService.findByToken(token, user);
    }
}
