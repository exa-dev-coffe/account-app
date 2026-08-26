package com.account_service.be.account;

import com.account_service.be.role.RoleModel;
import com.account_service.be.utils.commons.BaseModal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "tm_accounts")
@SQLDelete(sql = "UPDATE tm_accounts SET deleted_at = NOW() WHERE user_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class AccountModel extends BaseModal {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "photo", nullable = true)
    private String photo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleModel role;

    @Column(name = "deleted_at", nullable = true)
    private java.util.Date deletedAt;

    @Column(name = "deleted_by", nullable = true)
    private Integer deletedBy;
}