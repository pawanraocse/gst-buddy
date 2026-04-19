package com.learning.backendservice.service;

import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;
import com.learning.backendservice.engine.AnalysisMode;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.SharedResources;
import com.learning.backendservice.repository.LateFeeReliefWindowRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final LateFeeReliefWindowRepository reliefWindowRepository;

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

        log.debug("ContextEnricher loaded {} relief window buckets for tenant={}",
                reliefWindows.size(), context.tenantId());

        return new SharedResources(Collections.unmodifiableMap(reliefWindows), Map.of());
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
