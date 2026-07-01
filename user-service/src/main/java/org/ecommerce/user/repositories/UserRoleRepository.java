package org.ecommerce.user.repositories;

import org.ecommerce.user.entity.RoleEntity;
import org.ecommerce.user.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {
    @Query("""
            SELECT DISTINCT p.name
            FROM UserRoleEntity ur
            JOIN ur.role r
            JOIN r.permissions rp
            JOIN rp.permission p
            WHERE ur.user.email = :email
            """)
    List<String> findAllPermissionByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END " +
            "FROM UserRoleEntity ur " +
            "WHERE ur.user.id = :userId AND ur.role.id = :roleId")
    boolean existByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);


    @Query("""
                SELECT ur.role
                FROM UserRoleEntity ur
                WHERE ur.user.id = :userId
            """)
    List<RoleEntity> findAllRolesByUserId(Long userId);
}
