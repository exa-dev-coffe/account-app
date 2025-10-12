package com.account_service.be.refreshToken;

import com.account_service.be.account.AccountModel;
import com.account_service.be.refreshToken.dto.AccountCacheDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void addRefreshToken(String refreshToken, AccountModel accountModel) {
        RefreshTokenModel refreshTokenModel = new RefreshTokenModel();
        refreshTokenModel.setToken(refreshToken);
        refreshTokenModel.setUserId(accountModel);
        refreshTokenModel.setExpiryAt(new java.util.Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)); // 7 days
        refreshTokenRepository.save(refreshTokenModel);
        AccountCacheDto accountCacheDto = new AccountCacheDto();
        accountCacheDto.setId(accountModel.getUserId());
        accountCacheDto.setEmail(accountModel.getEmail());
        accountCacheDto.setFullName(accountModel.getFullName());
        accountCacheDto.setRole(accountModel.getRole().getRoleName());
        this.redisTemplate.opsForValue().set("refreshToken:" + refreshToken, accountCacheDto);
    }

    public void deleteRefreshTokenByToken(String refreshToken) {
        int deletedCount = this.refreshTokenRepository.deleteByToken(refreshToken);
        if (deletedCount == 0) {
            log.warn("No refresh token found to delete for token: {}", refreshToken);
        } else {
            this.redisTemplate.delete("refreshToken:" + refreshToken);
        }
    }

    public void deleteRefreshTokenByUser(AccountModel user) {
        int deletedCount = this.refreshTokenRepository.deleteByUserId(user);
        if (deletedCount == 0) {
            log.warn("No refresh tokens found to delete for user ID: {}", user.getUserId());
        }
    }


    public AccountCacheDto findByToken(String token, AccountModel user) {
        AccountCacheDto tokenOnRedis = (AccountCacheDto) this.redisTemplate.opsForValue().get("refreshToken:" + token);
        if (tokenOnRedis != null) {
            return tokenOnRedis;
        }

        RefreshTokenModel refreshTokenModel = this.refreshTokenRepository.findByToken(token);
        if (refreshTokenModel != null) {
            AccountCacheDto accountCacheDto = new AccountCacheDto();
            accountCacheDto.setId(user.getUserId());
            accountCacheDto.setEmail(user.getEmail());
            accountCacheDto.setFullName(user.getFullName());
            accountCacheDto.setRole(user.getRole().getRoleName());
            this.redisTemplate.opsForValue().set("refreshToken:" + token, accountCacheDto, Duration.ofHours(1));
            return accountCacheDto;
        } else {
            return null;
        }
    }
}
