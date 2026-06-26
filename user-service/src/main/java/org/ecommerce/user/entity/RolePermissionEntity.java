package org.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "role_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}))
public class RolePermissionEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;
    @ManyToOne(optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;

}
