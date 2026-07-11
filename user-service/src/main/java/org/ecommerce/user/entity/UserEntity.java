package org.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ecommerce.user.utils.Gender;

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
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Column(unique = true)
    private String mobile;
    private boolean active = true;
    private LocalDate dateOfBirth;
    @OneToMany(mappedBy = "user")
    private Set<UserRoleEntity> roles;

}
