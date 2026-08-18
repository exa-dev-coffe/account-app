package com.account_service.be.account;

import com.account_service.be.account.dto.NamesResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<AccountModel, Integer>, JpaSpecificationExecutor<AccountModel> {
    AccountModel findByEmail(String email);

    // Ambil entity langsung
    AccountModel findByUserId(Integer userId);

    // Ambil projection/DTO fleksibel
    <T> T findByUserId(Integer userId, Class<T> type);

    @Query("SELECT new com.account_service.be.account.dto.NamesResponseDto(a.userId, a.fullName, a.email) FROM AccountModel a WHERE a.userId IN :userIds")
    List<NamesResponseDto> findByUserIdIn(@Param("userIds") Integer[] userIds);

}
