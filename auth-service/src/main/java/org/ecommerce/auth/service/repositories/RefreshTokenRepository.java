package org.ecommerce.auth.service.repositories;

import org.ecommerce.auth.service.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByUserAccountIdAndExpiredAtAfter(Long userAccountId, LocalDateTime expiredAt);

    Optional<RefreshTokenEntity> findByTokenAndActiveTrueAndExpiredAtAfter(String token, LocalDateTime expiredAt);

    List<RefreshTokenEntity> findAllByUserAccountIdAndActiveTrue(Long userAccountId);
}
