package org.ecommerce.auth.service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@EqualsAndHashCode(exclude = "refreshTokens", callSuper = false)
@Entity
@Data
@Table(name = "user_credentials")
@NoArgsConstructor
public class UserCredentialEntity extends BaseEntity {

    public UserCredentialEntity(Long id) {
        this.id=id;
    }
    @Column(name = "user_account_id", nullable = false,unique = true)
    private Long userAccountId; // userAccountId from user-management-service
    private String password;
    private LocalDateTime lockedExpiryAt;
    private String lastLoginIpAddress;
    private LocalDateTime lastLoginAt;
    private boolean active = true;
    @OneToMany(mappedBy = "userCredential", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshTokenEntity> refreshTokens;

    public void addRefreshToken(RefreshTokenEntity refreshTokenEntity) {
        refreshTokens.add(refreshTokenEntity);
        refreshTokenEntity.setUserCredential(this);
    }

    @PreUpdate
    public void prePersist() {
        lastLoginAt = LocalDateTime.now();
    }

}
