package com.learning.authservice.security.repository;

import com.learning.authservice.security.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {

    /**
     * Fetch all permission IDs granted to a set of roles.
     */
    @Query("SELECT rp.permissionId FROM RolePermission rp WHERE rp.roleId IN :roleIds")
    Set<String> findPermissionIdsByRoleIds(@Param("roleIds") Iterable<String> roleIds);
}
