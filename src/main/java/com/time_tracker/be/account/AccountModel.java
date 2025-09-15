package com.time_tracker.be.account;

import com.time_tracker.be.balance.BalanceModel;
import com.time_tracker.be.common.BaseModal;
import com.time_tracker.be.common.RoleModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tm_accounts")
public class AccountModel extends BaseModal {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "photo", nullable = true)
    private String photo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleModel roleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "balance_id", nullable = true)
    private BalanceModel balanceId;
}