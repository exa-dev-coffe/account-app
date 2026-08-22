package com.account_service.be.account;

import com.account_service.be.account.dto.BaristaResponseDto;
import com.account_service.be.account.dto.MeResponseDto;
import com.account_service.be.account.dto.NamesResponseDto;
import com.account_service.be.account.dto.TokenResponseDto;
import com.account_service.be.account.projection.AccountProjection;
import com.account_service.be.exception.BadRequestException;
import com.account_service.be.exception.NotAuthorizedException;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final RabbitmqService rabbitmqService;
    private final String frontendUrl;
    private final ResetTokenPasswordService resetTokenPasswordService;
    private final RedisTemplate<String, Object> redisTemplate;

    public AccountService(AccountRepository accountRepository, JwtService jwtService, @Value("${spring.security.oauth2.authorizationserver.client.google.client-id}") String clientId, RefreshTokenService refreshTokenService, RabbitmqService rabbitmqService, @Value("${app.frontend.url}") String frontendUrl, ResetTokenPasswordService resetTokenPasswordService, RedisTemplate<String, Object> redisTemplate, @Value("${spring.security.oauth2.authorizationserver.client.google.client-secret}") String clientSecret) {
        this.accountRepository = accountRepository;
        this.resetTokenPasswordService = resetTokenPasswordService;
        this.rabbitmqService = rabbitmqService;
        this.CLIENT_SECRET = clientSecret;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.CLIENT_ID = clientId;
        this.frontendUrl = frontendUrl;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> login(String email, String password) {
        AccountModel user = this.accountRepository.findByEmail(email);

        if (user == null) {
            throw new BadRequestException("Incorrect password or email not registered");
        }

        boolean passwordMatch = PasswordUtils.matches(password, user.getPassword());

        if (!passwordMatch) {
            throw new BadRequestException("Incorrect password or email not registered");
        }

        TokenResponseDto data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days
        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Login successful", data);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> loginGoogle(String tokenTemp) throws Exception {
        Object tempTokenObj = redisTemplate.opsForValue().get("exchangeToken:" + tokenTemp);
        if (tempTokenObj == null) {
            throw new NotAuthorizedException("Temporary token is invalid or has expired");
        }
        TokenResponseDto data = (TokenResponseDto) tempTokenObj;
        redisTemplate.delete("exchangeToken:" + tokenTemp);

        this.refreshTokenService.addRefreshToken(data.getRefreshToken(), this.accountRepository.findByUserId(jwtService.getClaims(data.getAccessToken()).get("userId", Integer.class)));
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days
        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Login successful", data);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public String loginGoogleCallback(String code, String redirectUrl) throws Exception {

        String idTokenString = GoogleTokenUtils.exchangeCodeForTokens(code, CLIENT_ID, CLIENT_SECRET, redirectUrl);

        GoogleIdToken.Payload payload = GoogleTokenUtils.verifyGoogleToken(idTokenString, CLIENT_ID);

        String email = (String) payload.get("email");
        boolean emailVerified = (boolean) payload.get("email_verified");
        String aud = (String) payload.get("aud");

        if (!emailVerified) {
            throw new Exception("Email not verified");
        }

        if (!CLIENT_ID.equals(aud)) {
            throw new Exception("Invalid audience");
        }

        try {

            AccountModel user = this.accountRepository.findByEmail(email);
            if (user == null) {
                throw new Exception("Email not found");
            }
            // generate token for authorize call back temporary 5 minutes to exchange to real token
            String tokenTemporary = jwtService.createToken(user, TokenType.EXCHANGE);

            TokenResponseDto data = this.createTokenResponse(user);

            redisTemplate.opsForValue().set("exchangeToken:" + tokenTemporary, data, java.time.Duration.ofMinutes(5));

            return tokenTemporary;
        } catch (Exception e) {
            if (e.getMessage().equals("Email not found") || e.getMessage().equals("Invalid audience") || e.getMessage().equals("Email not verified") || e.getMessage().equals("Google login failed. Please try again.")) {
                throw e;
            } else {
                log.error("Error during Google login callback: {}", e.getMessage());
                throw new Exception("Google login failed. Please try again.");
            }
        }

    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> register(String email, String password, String name, Integer userId, Integer type, String code) {
        AccountModel existingUser = this.accountRepository.findByEmail(email);
        if (existingUser != null) {
            throw new BadRequestException("Email already registered");
        }

        // If type == 2 (Customer registration), we require code verification
        if (type == 2) {
            if (code == null || code.trim().isEmpty()) {
                throw new BadRequestException("Verification code is required");
            }
            String codeKey = "register:code:" + email;
            Object savedCode = redisTemplate.opsForValue().get(codeKey);
            if (savedCode == null || !savedCode.toString().equals(code)) {
                throw new BadRequestException("Verification code is incorrect or has expired");
            }
            redisTemplate.delete(codeKey);
        }

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
        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Registration successful", data);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<Object>> sendVerificationCode(String email) throws Exception {
        AccountModel existingUser = this.accountRepository.findByEmail(email);
        if (existingUser != null) {
            throw new BadRequestException("Email already registered");
        }

        String sendCountKey = "register:sendCount:" + email;
        Object countObj = redisTemplate.opsForValue().get(sendCountKey);
        int count = 0;
        if (countObj != null) {
            if (countObj instanceof Integer) {
                count = (Integer) countObj;
            } else if (countObj instanceof Long) {
                count = ((Long) countObj).intValue();
            } else {
                count = Integer.parseInt(countObj.toString());
            }
        }

        if (count >= 3) {
            throw new TooManyRequestException("Maximum limit of verification code requests (3 times) for today has been reached.");
        }

        // Generate 6-digit random code
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // Store count with 24 hours expiry
        if (countObj == null) {
            redisTemplate.opsForValue().set(sendCountKey, 1, java.time.Duration.ofHours(24));
        } else {
            redisTemplate.opsForValue().set(sendCountKey, count + 1, java.time.Duration.ofHours(24));
        }

        // Store code in Redis with 10 mins expiry
        String codeKey = "register:code:" + email;
        redisTemplate.opsForValue().set(codeKey, code, java.time.Duration.ofMinutes(10));

        // Publish to rabbitmq
        String jsonMessage = String.format("{\"to\":\"%s\",\"subject\":\"Email Verification Code - Diskusi Coffee\",\"code\":\"%s\"}", email, code);
        this.rabbitmqService.sendMessage(
                "Email Verification Code",
                "emailQueue.verificationCode",
                "email.queue",
                ExchangeType.DIRECT,
                null,
                jsonMessage,
                true,
                false,
                false,
                null
        );

        ResponseModel<Object> response = new ResponseModel<>(true, "Verification code has been sent to your email.", null);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<Object>> loginGooglePopup(String code) throws Exception {
        // Exchange auth code for ID Token. For popup flow, redirectUri must be "postmessage".
        String idTokenString = GoogleTokenUtils.exchangeCodeForTokens(code, CLIENT_ID, CLIENT_SECRET, "postmessage");
        GoogleIdToken.Payload payload = GoogleTokenUtils.verifyGoogleToken(idTokenString, CLIENT_ID);

        String email = (String) payload.get("email");
        boolean emailVerified = (boolean) payload.get("email_verified");
        String aud = (String) payload.get("aud");

        if (!emailVerified) {
            throw new BadRequestException("Google email not verified");
        }

        if (!CLIENT_ID.equals(aud)) {
            throw new BadRequestException("Audience mismatch");
        }

        AccountModel user = this.accountRepository.findByEmail(email);
        if (user == null) {
            // User does not exist, require password setting/registration
            String name = (String) payload.get("name");
            String registrationToken = jwtService.createRegistrationToken(email, name);

            HashMap<String, Object> responseData = new HashMap<>();
            responseData.put("registerRequired", true);
            responseData.put("registrationToken", registrationToken);
            responseData.put("email", email);
            responseData.put("fullName", name);

            ResponseModel<Object> response = new ResponseModel<>(true, "Google registration required", responseData);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        // User exists, login directly
        TokenResponseDto data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days

        HashMap<String, Object> responseData = new HashMap<>();
        responseData.put("registerRequired", false);
        responseData.put("authData", data);

        ResponseModel<Object> response = new ResponseModel<>(true, "Login successful", responseData);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> googleRegister(String registrationToken, String password) throws Exception {
        Claims claims = jwtService.getClaims(registrationToken);
        if (!claims.get("type").equals(TokenType.REGISTRATION.name())) {
            throw new BadRequestException("Registration token is invalid");
        }

        String email = claims.get("email", String.class);
        String fullName = claims.get("fullName", String.class);

        AccountModel existingUser = this.accountRepository.findByEmail(email);
        if (existingUser != null) {
            throw new BadRequestException("Email already registered");
        }

        AccountModel user = new AccountModel();
        RoleModel role = new RoleModel();
        role.setRoleId(2); // Customer
        user.setRole(role);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(PasswordUtils.hashPassword(password));
        user.setPhoto(null);
        user.setCreatedBy(null);

        this.accountRepository.save(user);

        TokenResponseDto data = this.createTokenResponse(user);
        this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Google registration successful", data);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token not found");
        }

        Claims claims = jwtService.getClaims(refreshToken);

        if (!claims.get("type").equals(TokenType.REFRESH.name())) {
            throw new BadRequestException("Refresh token is invalid");
        }

        boolean isExpired = claims.getExpiration().before(new Date());

        if (isExpired) {
            throw new NotAuthorizedException("Refresh token has expired");
        }

        AccountModel user = this.accountRepository.findByUserId(Integer.parseInt(claims.get("userId").toString()));

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        AccountCacheDto tokenFromDb = this.getTokenFromDb(refreshToken, user);
        if (tokenFromDb == null) {
            throw new BadRequestException("Refresh token is invalid");
        }

        boolean isWillBeExpired = claims.getExpiration().before(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 3000)); // 3 days

        if (!isWillBeExpired) {
            String newAccessToken = jwtService.createToken(user, TokenType.ACCESS);
            TokenResponseDto data = new TokenResponseDto();
            data.setAccessToken(newAccessToken);
            data.setRefreshToken(refreshToken);
            ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Refresh token successful", data);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        } else {
            TokenResponseDto data = this.createTokenResponse(user);
            this.refreshTokenService.addRefreshToken(data.getRefreshToken(), user);
            this.refreshTokenService.deleteRefreshTokenByToken(refreshToken);
            ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", data.getRefreshToken(), 7 * 24 * 60 * 60); // 7 days

            ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "Refresh token successful", data);
            return ResponseEntity.status(HttpStatus.OK)
                    .header("Set-Cookie", cookie.toString())
                    .body(response);
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<Object>> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token not found");
        }
        this.refreshTokenService.deleteRefreshTokenByToken(refreshToken);
        ResponseCookie cookie = this.createHttpOnlyCookie("refreshToken", "", 0); // expire the cookie
        ResponseModel<Object> response = new ResponseModel<>(true, "Logout successful", null);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    public ResponseEntity<ResponseModel<MeResponseDto>> me(CurrentUserDto user) {
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        AccountProjection data = this.accountRepository.findByUserId(user.getUserId(), AccountProjection.class);
        if (data == null) {
            throw new NotFoundException("User not found");
        }
        MeResponseDto me = new MeResponseDto();
        me.setUserId(data.getUserId());
        me.setEmail(data.getEmail());
        me.setFullName(data.getFullName());
        me.setPhoto(data.getPhoto());
        me.setRole(data.getRole().getRoleName());
        ResponseModel<MeResponseDto> response = new ResponseModel<>(true, "User data found", me);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    public ResponseEntity<ResponseModel<String>> forgotPassword(String email) throws Exception {
        AccountModel user = this.accountRepository.findByEmail(email);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (this.resetTokenPasswordService.checkWasLimitOneDay(user)) {
            throw new TooManyRequestException("You have reached the maximum daily limit for password reset requests. Please try again tomorrow.");
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
                "email.queue",
                ExchangeType.DIRECT,
                null,
                jsonMessage,
                true,
                false,
                false,
                null
        );


        ResponseModel<String> response = new ResponseModel<>(true, "Password reset link has been sent to your email if registered.", null);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> resetPassword(String token, String newPassword) throws Exception {
        Claims claims = jwtService.getClaims(token);

        boolean isExpired = claims.getExpiration().before(new Date());

        if (isExpired) {
            throw new BadRequestException("Token expired, Please request a new password reset.");
        }

        if (!claims.get("type").equals(TokenType.RESET_PASSWORD.name())) {
            throw new BadRequestException("Token is invalid");
        }

        AccountModel user = this.accountRepository.findByUserId(Integer.parseInt(claims.get("userId").toString()));

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.setPassword(PasswordUtils.hashPassword(newPassword));
        this.accountRepository.save(user);

        this.resetTokenPasswordService.updateResetToken(token, user);

        HashMap<String, Object> emailPayload = new HashMap<>();
        emailPayload.put("to", user.getEmail());
        emailPayload.put("subject", "Reset Password Successful");
        ObjectMapper mapper = new ObjectMapper();
        String jsonMessage = mapper.writeValueAsString(emailPayload);

        this.rabbitmqService.sendMessage(
                "Email Reset Password Success",
                "emailQueue.resetPasswordSuccess",
                "email.queue",
                ExchangeType.DIRECT,
                null,
                jsonMessage,
                true,
                false,
                false,
                null
        );


        ResponseModel<String> response = new ResponseModel<>(true, "Password changed successfully", null);
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

        ResponseModel<PaginationResponseDto<BaristaResponseDto>> response = new ResponseModel<>(true, "Barista data found", responsePagination);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> deleteBarista(Integer baristaId) {
        AccountModel user = this.accountRepository.findById(baristaId).orElse(null);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        if (user.getRole().getRoleId() != 3) {
            throw new BadRequestException("User is not a barista");
        }

        this.refreshTokenService.deleteRefreshTokenByUser(user);
        this.accountRepository.delete(user);

        ResponseModel<String> response = new ResponseModel<>(true, "Barista deleted successfully", null);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TokenResponseDto>> updateUser(String refreshToken, Integer userId, String fullName, String photo) {
        AccountModel user = this.accountRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new NotFoundException("User not found");
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

        ResponseModel<TokenResponseDto> response = new ResponseModel<>(true, "User updated successfully", data);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Set-Cookie", cookie.toString())
                .body(response);
    }

    public List<NamesResponseDto> getNamesByUserIds(Integer[] userIds) {
        return this.accountRepository.findByUserIdIn(userIds);
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
