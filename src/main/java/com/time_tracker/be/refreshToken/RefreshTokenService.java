package com.time_tracker.be.refreshToken;

import com.time_tracker.be.account.AccountModel;
import com.time_tracker.be.refreshToken.dto.AccountCacheDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
        accountCacheDto.setRole(accountModel.getRoleId().getRoleName());
        this.redisTemplate.opsForValue().set("refreshToken:" + refreshToken, accountCacheDto);
    }

    public AccountCacheDto findByTokenAndUserId(String token, AccountModel user) {
        AccountCacheDto tokenOnRedis = (AccountCacheDto) this.redisTemplate.opsForValue().get("refreshToken:" + token);
        if (tokenOnRedis != null) {
            return tokenOnRedis.getId() == user.getUserId() ? tokenOnRedis : null;
        }
        RefreshTokenModel refreshTokenModel = this.refreshTokenRepository.findByTokenAndUserId(token, user);
        if (refreshTokenModel != null) {
            AccountCacheDto accountCacheDto = new AccountCacheDto();
            accountCacheDto.setId(user.getUserId());
            accountCacheDto.setEmail(user.getEmail());
            accountCacheDto.setFullName(user.getFullName());
            accountCacheDto.setRole(user.getRoleId().getRoleName());
            this.redisTemplate.opsForValue().set("refreshToken:" + token, accountCacheDto);
            return accountCacheDto;
        } else {
            return null;
        }
    }
}
