package org.ecommerce.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "permissions")
public class PermissionEntity extends BaseEntity {
    private String name;
    private String description;
    private boolean active = true;
    @OneToMany(mappedBy = "permission")
    private Set<RolePermissionEntity> roles;
}
