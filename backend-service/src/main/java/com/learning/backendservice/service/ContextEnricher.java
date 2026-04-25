package com.learning.backendservice.service;

import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;
import com.learning.backendservice.engine.AnalysisMode;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.SharedResources;
import com.learning.backendservice.entity.ReconToleranceConfig;
import com.learning.backendservice.entity.Rule86bConfig;
import com.learning.backendservice.repository.LateFeeReliefWindowRepository;
import com.learning.backendservice.repository.ReconToleranceConfigRepository;
import com.learning.backendservice.repository.Rule86bConfigRepository;
import com.learning.backendservice.domain.rule86b.Rule86bConfigSnapshot;
import com.learning.backendservice.infra.portal.PortalClient;
import com.learning.backendservice.domain.gstr2a.GstinStatusSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pre-pipeline enrichment step that loads shared DB resources into {@link SharedResources}.
 *
 * <p>Called <em>once</em> by {@code AuditRunOrchestrator} after the initial context is built
 * and before rule execution begins. The loaded resources are then attached to the context via
 * {@link AuditContext#withSharedResources(SharedResources)}.
 *
 * <p>This pattern keeps all {@link com.learning.backendservice.engine.AuditRule} implementations
 * database-free — they read from the pre-loaded context, never from repositories.
 */
@Service
@RequiredArgsConstructor
public class ContextEnricher {

    private static final Logger log = LoggerFactory.getLogger(ContextEnricher.class);

    private static final BigDecimal DEFAULT_TOLERANCE_AMOUNT  = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_TOLERANCE_PERCENT = new BigDecimal("0.0001");

    private final LateFeeReliefWindowRepository    reliefWindowRepository;
    private final ReconToleranceConfigRepository   reconToleranceRepository;
    private final Rule86bConfigRepository          rule86bConfigRepository;
    private final PortalClient                     portalClient;

    /**
     * Load all shared DB resources needed for the given context.
     * Only fetches resource types relevant to the uploaded document set.
     *
     * @param context initial context (without shared resources)
     * @return populated {@link SharedResources} ready to attach to the context
     */
    @Transactional(readOnly = true)
    public SharedResources loadResources(AuditContext context) {
        Map<String, List<ReliefWindowSnapshot>> reliefWindows = new HashMap<>();

        if (context.analysisMode() == AnalysisMode.LEDGER_ANALYSIS
                || context.hasDocument(DocumentType.GSTR_1)) {
            loadReliefWindows(reliefWindows, "GSTR1", "GSTR_1");
        }
        if (context.hasDocument(DocumentType.GSTR_3B)) {
            loadReliefWindows(reliefWindows, "GSTR3B", "GSTR_3B");
        }
        if (context.hasDocument(DocumentType.GSTR_9)) {
            loadReliefWindows(reliefWindows, "GSTR9", "GSTR_9");
        }

        // Load per-tenant recon tolerance; fall back to DEFAULT row if not configured.
        BigDecimal toleranceAmount  = DEFAULT_TOLERANCE_AMOUNT;
        BigDecimal tolerancePercent = DEFAULT_TOLERANCE_PERCENT;

        ReconToleranceConfig toleranceCfg = reconToleranceRepository
                .findByTenantId(context.tenantId())
                .or(() -> reconToleranceRepository.findByTenantId("DEFAULT"))
                .orElse(null);

        if (toleranceCfg != null) {
            toleranceAmount  = toleranceCfg.getToleranceAmount();
            tolerancePercent = toleranceCfg.getTolerancePercent();
        }

        // Load Rule 86B config
        Rule86bConfig rule86bCfg = null;
        if (context.hasDocument(DocumentType.GSTR_3B)) {
            rule86bCfg = rule86bConfigRepository.findByTenantId(context.tenantId())
                    .or(() -> rule86bConfigRepository.findByTenantId("DEFAULT"))
                    .orElse(null);
        }
        Rule86bConfigSnapshot rule86bConfigSnapshot = rule86bCfg != null 
                ? new Rule86bConfigSnapshot(rule86bCfg.getTurnoverThreshold(), rule86bCfg.getCashPercentFloor(), rule86bCfg.getEffectiveFrom()) 
                : Rule86bConfigSnapshot.defaults();

        log.debug("ContextEnricher: tenant={} reliefBuckets={} reconTolerance={}₹/{}% rule86bThreshold={}",
                context.tenantId(), reliefWindows.size(),
                toleranceAmount, tolerancePercent.multiply(new BigDecimal("100")),
                rule86bConfigSnapshot.turnoverThreshold());

        Map<String, GstinStatusSnapshot> gstinStatusMap = new HashMap<>();
        Set<String> uniqueGstins = new HashSet<>();
        
        for (DocumentType docType : List.of(DocumentType.GSTR_2A, DocumentType.GSTR_2B)) {
            context.getDocument(docType).ifPresent(doc -> {
                Map<String, Object> fields = doc.extractedFields();
                if (fields != null && fields.get("suppliers") instanceof List<?> rawSuppliers) {
                    for (Object obj : rawSuppliers) {
                        if (obj instanceof Map<?, ?> supMap) {
                            String supGstin = (String) supMap.get("supplier_gstin");
                            if (supGstin != null) {
                                uniqueGstins.add(supGstin);
                            }
                        }
                    }
                }
            });
        }
        
        for (String gstin : uniqueGstins) {
            try {
                PortalClient.GstinStatus statusInfo = portalClient.getGstinStatus(gstin);
                gstinStatusMap.put(gstin, new GstinStatusSnapshot(
                        statusInfo.status(),
                        statusInfo.cancellationDate() != null ? statusInfo.cancellationDate().toString() : null
                ));
            } catch (Exception e) {
                // Non-fatal: portal API failure must not abort the audit pipeline.
                // Default to ACTIVE so the audit proceeds; reviewer is informed via log.
                log.warn("ContextEnricher: failed to fetch GSTIN status for {} — defaulting to ACTIVE. Cause: {}",
                        gstin, e.getMessage());
                gstinStatusMap.put(gstin, new GstinStatusSnapshot("ACTIVE", null));
            }
        }

        return new SharedResources(
                Collections.unmodifiableMap(reliefWindows),
                Map.of(),
                toleranceAmount,
                tolerancePercent,
                rule86bConfigSnapshot,
                Collections.unmodifiableMap(gstinStatusMap)
        );
    }

    private void loadReliefWindows(
            Map<String, List<ReliefWindowSnapshot>> target,
            String returnType,
            String keyPrefix) {

        for (String appliesTo : List.of("NIL", "NON_NIL")) {
            List<ReliefWindowSnapshot> snapshots = reliefWindowRepository
                    .findByReturnTypeAndAppliesTo(returnType, appliesTo)
                    .stream()
                    .map(r -> new ReliefWindowSnapshot(
                            r.getNotificationNo(),
                            r.getFeeCgstPerDay(),
                            r.getFeeSgstPerDay(),
                            r.getMaxCapCgst(),
                            r.getMaxCapSgst()))
                    .toList();
            target.put(keyPrefix + "_" + appliesTo, snapshots);
        }
    }
}

