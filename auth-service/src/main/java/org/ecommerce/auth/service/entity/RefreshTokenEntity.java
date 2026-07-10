package org.ecommerce.auth.service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshTokenEntity extends BaseEntity {
    @Column(columnDefinition = "TEXT")
    private String token;
    private LocalDateTime expiredAt;
    private Long userAccountId;
    private Boolean active;
    @ManyToOne(optional = false)
    @JoinColumn(name = "credential_id")
    private UserCredentialEntity userCredential;

}
