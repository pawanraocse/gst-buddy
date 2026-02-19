package com.learning.authservice.credit.repository;

import com.learning.authservice.credit.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Plan} entities.
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * Returns all active plans, ordered by sort_order for display.
     */
    List<Plan> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * Find an active plan by its unique name.
     */
    Optional<Plan> findByNameAndIsActiveTrue(String name);
}
