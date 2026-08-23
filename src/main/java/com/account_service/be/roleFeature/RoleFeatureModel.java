package com.account_service.be.roleFeature;

import com.account_service.be.feature.FeatureModel;
import com.account_service.be.role.RoleModel;
import com.account_service.be.utils.commons.BaseModal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tm_role_features", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"role_id", "feature_id"})
})
public class RoleFeatureModel extends BaseModal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleModel role;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private FeatureModel feature;

    @Column(name = "can_view", nullable = false)
    private boolean canView = false;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = false;

    @Column(name = "can_edit", nullable = false)
    private boolean canEdit = false;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;
}
