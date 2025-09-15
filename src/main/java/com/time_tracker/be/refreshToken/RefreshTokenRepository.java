package com.time_tracker.be.refreshToken;

import com.time_tracker.be.account.AccountModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenModel, Integer> {
    RefreshTokenModel findByTokenAndUserId(String token, AccountModel userId);

}
