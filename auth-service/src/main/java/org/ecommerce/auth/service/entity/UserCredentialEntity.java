package org.ecommerce.auth.service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Set;

@EqualsAndHashCode(exclude = "refreshTokens")
@Entity
@Data
@Table(name = "user_credentials")
public class UserCredentialEntity extends BaseEntity {
    @Column(name = "user_account_id", nullable = false,unique = true)
    private Long userAccountId; // userAccountId from user-management-service
    private String password;
    private LocalDateTime lockedExpiryAt;
    private String lastLoginIpAddress;
    private LocalDateTime lastLoginAt;
    private boolean status;
    @OneToMany(mappedBy = "userCredential", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshTokenEntity> refreshTokens;

    @PrePersist
    public void prePersist() {
        lastLoginAt = LocalDateTime.now();
    }

}
