package org.ecommerce.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Setter
@Getter
@Table(name = "roles")
public class RoleEntity extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String name;
    private String description;
    private boolean active = true;
    @OneToMany(mappedBy = "role")
    private Set<RolePermissionEntity> permissions;

}
