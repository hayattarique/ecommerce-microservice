package org.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.ecommerce.user.utils.Gender;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true,exclude = "roles")
@Entity
@Getter
@Setter
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
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRoleEntity> roles = new HashSet<>();

}
