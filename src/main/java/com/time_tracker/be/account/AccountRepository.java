package com.time_tracker.be.account;

import com.time_tracker.be.account.projection.BaristaProjection;
import com.time_tracker.be.role.RoleModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountModel, Integer> {
    AccountModel findByEmail(String email);

    // Ambil entity langsung
    AccountModel findByUserId(Integer userId);

    // Ambil projection/DTO fleksibel
    <T> T findByUserId(Integer userId, Class<T> type);

    Page<BaristaProjection> findByRoleAndEmailLikeIgnoreCase(
            RoleModel role,
            String emailPattern,
            Pageable pageable
    );

}
