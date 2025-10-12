package com.account_service.be.tokenResetPassword;

import com.account_service.be.account.AccountModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository

public interface ResetTokenPasswordRepository extends JpaRepository<ResetTokenPasswordModel, Integer> {
    ResetTokenPasswordModel findByToken(String token);

    List<ResetTokenPasswordModel> findByUserIdAndCreatedAtBetween(
            AccountModel userId,
            Date createdAtAfter,
            Date createdAtBefore
    );


    int deleteByToken(String token);

}
