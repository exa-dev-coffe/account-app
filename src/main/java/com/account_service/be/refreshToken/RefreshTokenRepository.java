package com.account_service.be.refreshToken;

import com.account_service.be.account.AccountModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenModel, Integer> {

    int deleteByToken(String token);

    RefreshTokenModel findByToken(String token);

    int deleteByUserId(AccountModel userId);
}
