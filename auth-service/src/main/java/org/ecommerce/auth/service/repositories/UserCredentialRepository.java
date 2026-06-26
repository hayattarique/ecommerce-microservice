package org.ecommerce.auth.service.repositories;

import org.ecommerce.auth.service.entity.UserCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, Long> {
    @Query("FROM UserCredentialEntity u where u.userAccountId=:userAccountId")
    Optional<UserCredentialEntity> findByUserAccountIdAndActiveIsTrue(@Param("userAccountId") Long userAccountId);

}
