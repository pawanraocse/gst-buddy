package com.learning.backendservice.repository;

import com.learning.backendservice.entity.Rule86bConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface Rule86bConfigRepository extends JpaRepository<Rule86bConfig, UUID> {
    Optional<Rule86bConfig> findByTenantId(String tenantId);
}
