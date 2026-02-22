package com.learning.authservice.security.repository;

import com.learning.authservice.security.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(String userId);

    List<UserRole> findByUserIdAndTenantId(String userId, String tenantId);

    boolean existsByUserIdAndRoleId(String userId, String roleId);

    void deleteByUserIdAndRoleId(String userId, String roleId);
}
