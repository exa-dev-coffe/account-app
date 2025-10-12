package com.account_service.be.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountModel, Integer>, JpaSpecificationExecutor<AccountModel> {
    AccountModel findByEmail(String email);

    // Ambil entity langsung
    AccountModel findByUserId(Integer userId);

    // Ambil projection/DTO fleksibel
    <T> T findByUserId(Integer userId, Class<T> type);

}
