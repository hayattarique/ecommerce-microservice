package org.ecommerce.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "roles")
public class RoleEntity extends BaseEntity {
    private String name;
    private String description;
    @OneToMany(mappedBy = "role")
    private Set<RolePermissionEntity> permissions;

}
