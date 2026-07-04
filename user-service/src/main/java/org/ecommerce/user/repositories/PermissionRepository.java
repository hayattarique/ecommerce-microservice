package org.ecommerce.user.repositories;

import org.ecommerce.user.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    @Query("""
            SELECT DISTINCT p.name
            FROM UserRoleEntity ur
            JOIN ur.role r
            JOIN r.permissions rp
            JOIN rp.permission p
            WHERE ur.user.email = :email
            """)
    List<String> findAllPermissionsByEmail(@Param("email") String email);
}
