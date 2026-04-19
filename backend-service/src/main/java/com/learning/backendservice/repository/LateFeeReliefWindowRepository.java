package com.learning.backendservice.repository;

import com.learning.backendservice.entity.LateFeeReliefWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for CBIC notification-driven GST late fee relief windows.
 *
 * <p>Used exclusively by {@code AuditRunOrchestrator} to pre-resolve any
 * applicable relief window <em>before</em> passing input to the audit rule.
 * This keeps the {@link com.learning.backendservice.engine.AuditRule} implementations
 * database-free per their contract.
 */
@Repository
public interface LateFeeReliefWindowRepository extends JpaRepository<LateFeeReliefWindow, Integer> {

    /**
     * Find all relief windows applicable for a specific GSTR-1 filing.
     *
     * <p>A window matches when:
     * <ol>
     *   <li>The {@code arnDate} (actual filing date) falls within
     *       {@code start_date} to {@code end_date} (notification window).</li>
     *   <li>The {@code periodEndDate} (last day of the tax period) falls within
     *       {@code tax_period_from} to {@code tax_period_to}, OR both columns are NULL
     *       (meaning the notification covers all tax periods in that filing window).</li>
     *   <li>The {@code appliesTo} scope matches the filer type (NIL/NON_NIL/ALL).</li>
     * </ol>
     *
     * <p>The caller must call {@code .stream().findFirst()} to resolve the most
     * recently inserted matching record (highest id = most specific notification).
     *
     * @param arnDate       actual ARN date (filing date from parsed document)
     * @param periodEndDate last calendar day of the tax period (e.g. 2024-03-31 for Mar-2024)
     * @param appliesTo     "NIL" or "NON_NIL" — the actual filer type
     * @return ordered list of matching windows, highest id first
     */
    @Query("SELECT r FROM LateFeeReliefWindow r " +
           "WHERE r.returnType = 'GSTR1' " +
           "AND :arnDate BETWEEN r.startDate AND r.endDate " +
           "AND (:periodEndDate BETWEEN r.taxPeriodFrom AND r.taxPeriodTo " +
           "     OR r.taxPeriodFrom IS NULL) " +
           "AND (r.appliesTo = :appliesTo OR r.appliesTo = 'ALL') " +
           "ORDER BY r.id DESC")
    List<LateFeeReliefWindow> findApplicableGstr1Relief(
            @Param("arnDate")       LocalDate arnDate,
            @Param("periodEndDate") LocalDate periodEndDate,
            @Param("appliesTo")     String appliesTo
    );
    /**
     * Load all relief windows for a given return type and filer type.
     * Used by {@code ContextEnricher} to pre-load all windows in bulk before pipeline execution.
     *
     * @param returnType the GSTR return type string: "GSTR1", "GSTR3B", "GSTR9"
     * @param appliesTo  "NIL", "NON_NIL", or "ALL"
     * @return all matching relief windows ordered by id DESC (most specific first)
     */
    @Query("SELECT r FROM LateFeeReliefWindow r " +
           "WHERE r.returnType = :returnType " +
           "AND (r.appliesTo = :appliesTo OR r.appliesTo = 'ALL') " +
           "ORDER BY r.id DESC")
    List<LateFeeReliefWindow> findByReturnTypeAndAppliesTo(
            @Param("returnType") String returnType,
            @Param("appliesTo")  String appliesTo
    );
}
