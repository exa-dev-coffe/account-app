package com.time_tracker.be.account;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.time_tracker.be.common.ResponseModel;
import com.time_tracker.be.common.RoleModel;
import com.time_tracker.be.common.TokenType;
import com.time_tracker.be.exception.BadRequestException;
import com.time_tracker.be.exception.NotFoundException;
import com.time_tracker.be.security.JwtTokenProvider;
import com.time_tracker.be.utils.GoogleTokenUtils;
import com.time_tracker.be.utils.PasswordUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String CLIENT_ID;

    public AccountService(AccountRepository accountRepository, JwtTokenProvider jwtTokenProvider, @Value("${spring.security.oauth2.authorizationserver.client.google.client-id}") String clientId) {
        this.accountRepository = accountRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        CLIENT_ID = clientId;
    }


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
        ResponseModel<Object> response = new ResponseModel<>(true, "Login berhasil", data);
        return ResponseEntity.status(200).body(response);
    }

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
        ResponseModel<Object> response = new ResponseModel<>(true, "Login berhasil", data);
        return ResponseEntity.status(200).body(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<Object>> register(String email, String password, String name) {
        AccountModel user = new AccountModel();
        RoleModel role = new RoleModel();
        role.setRoleId(2); // role_id 2 adalah user
        user.setRoleId(role);
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(PasswordUtils.hashPassword(password));
        user.setRoleId(role);
        user.setPhoto(null);
        user.setBalanceId(null); // balance di-set null, karena akan di-set setelah registrasi di BalanceService
        this.accountRepository.save(user);
        HashMap<String, Object> data = this.createTokenResponse(user);
        ResponseModel<Object> response = new ResponseModel<>(true, "Registrasi berhasil", data);
        return ResponseEntity.status(201).body(response);
    }

    private HashMap<String, Object> createTokenResponse(AccountModel user) {
        String accessToken = jwtTokenProvider.createToken(user, TokenType.ACCESS);
        String refreshToken = jwtTokenProvider.createToken(user, TokenType.REFRESH);
        HashMap<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        return data;
    }
}
