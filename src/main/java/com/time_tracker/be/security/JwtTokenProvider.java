package com.time_tracker.be.security;

import com.time_tracker.be.account.AccountModel;
import com.time_tracker.be.common.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String SECRET_KEY;

    private SecretKey getSigningKey() {
        return new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS512.getJcaName()
        );
    }

    private Map<String, Object> createClaims(AccountModel user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("fullName", user.getFullName());
        claims.put("email", user.getEmail());
        claims.put("userId", user.getUserId());
        claims.put("role", user.getRoleId().getRoleName());
        return claims;
    }

    // Membuat token JWT
    public String createToken(AccountModel user, TokenType type) {
        return Jwts.builder()
                .setClaims(createClaims(user))
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(type == TokenType.ACCESS
                        ? new Date(System.currentTimeMillis() + 1000L * 60 * 15) // 15 menit
                        : new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7) // 7 hari
                )
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // Memvalidasi token JWT
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // Ambil claims dari token
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Ambil email dari token
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // Ambil token dari header Authorization
    public String resolveToken(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
