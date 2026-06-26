package org.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "user_roles",uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","role_id"}))
public class UserRoleEntity extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private UserEntity user;
    @ManyToOne
    @JoinColumn(name = "role_id",nullable = false)
    private RoleEntity role;
}
