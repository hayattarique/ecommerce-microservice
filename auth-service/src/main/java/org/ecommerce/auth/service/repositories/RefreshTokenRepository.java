package org.ecommerce.auth.service.repositories;

import org.ecommerce.auth.service.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity,Long> {
}
