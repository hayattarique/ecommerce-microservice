package org.ecommerce.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "users")
public class UserEntity extends BaseEntity {
    @Column(unique = true)
    private String email;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String displayName;
    private String gender;
    @Column(unique = true)
    private String mobile;
    private boolean status;
    private LocalDate dateOfBirth;
    @OneToMany(mappedBy = "user")
    private Set<UserRoleEntity> roles;

}
