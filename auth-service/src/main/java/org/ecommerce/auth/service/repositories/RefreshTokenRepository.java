package org.ecommerce.auth.service.repositories;

import org.ecommerce.auth.service.entity.RefreshTokenEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByUserAccountIdAndExpiredAtAfter(Long userAccountId, LocalDateTime expiredAt);

    Optional<RefreshTokenEntity> findByTokenAndActiveTrueAndExpiredAtAfter(String token, LocalDateTime expiredAt);

    List<RefreshTokenEntity> findAllByUserAccountIdAndActiveTrue(Long userAccountId);

    @Query("FROM RefreshTokenEntity r where r.active=false or r.expiredAt < CURRENT_TIMESTAMP ")
    List<RefreshTokenEntity> findAllByBatch(Pageable pageable);
}
