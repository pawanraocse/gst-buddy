package com.learning.backendservice.repository;

import com.learning.backendservice.entity.ReconToleranceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ReconToleranceConfig}.
 *
 * <p>Usage pattern in {@code ContextEnricher}:
 * <pre>{@code
 *   reconToleranceRepository.findByTenantId(tenantId)
 *       .or(() -> reconToleranceRepository.findByTenantId("DEFAULT"))
 *       .orElse(null);
 * }</pre>
 */
@Repository
public interface ReconToleranceConfigRepository extends JpaRepository<ReconToleranceConfig, UUID> {

    Optional<ReconToleranceConfig> findByTenantId(String tenantId);
}
