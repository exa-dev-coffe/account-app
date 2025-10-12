package com.account_service.be.tokenResetPassword;


import com.account_service.be.account.AccountModel;
import com.account_service.be.utils.commons.BaseModal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@AllArgsConstructor
@Table(name = "tm_reset_token_passwords")
@NoArgsConstructor
public class ResetTokenPasswordModel extends BaseModal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "token", columnDefinition = "TEXT", nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AccountModel userId;

    @Column(name = "used", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean used = false;

    @Column(name = "expiry_at", nullable = false)
    private Date expiryAt;
}
