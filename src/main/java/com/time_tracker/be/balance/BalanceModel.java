package com.time_tracker.be.balance;

import com.time_tracker.be.utils.commons.BaseModal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tr_balances")
public class BalanceModel extends BaseModal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int balanceId;

    @Column(name = "balance", nullable = false)
    private double balance;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "pin", nullable = false)
    private String pin;
}
