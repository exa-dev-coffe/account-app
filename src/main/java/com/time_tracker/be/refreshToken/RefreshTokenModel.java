package com.time_tracker.be.refreshToken;


import com.time_tracker.be.account.AccountModel;
import com.time_tracker.be.common.BaseModal;
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
@Table(name = "tm_refresh_tokens")
@NoArgsConstructor
public class RefreshTokenModel extends BaseModal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "token", columnDefinition = "TEXT", nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AccountModel userId;

    @Column(name = "expiry_at", nullable = false)
    private Date expiryAt;
}
