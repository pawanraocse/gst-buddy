package com.learning.backendservice.scheduler;

import com.learning.backendservice.repository.AuditRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Purges expired audit runs daily at 2:00 AM UTC.
 *
 * <p>Audit runs are created with an {@code expires_at} timestamp
 * (default: 7 days, configurable via {@code app.retention.days}).
 * This scheduler removes expired runs to prevent unbounded storage growth.
 *
 * <p>The {@code idx_audit_runs_expires} partial index ensures the DELETE
 * is O(expired_count) rather than a full-table scan.
 */
@Component
@EnableScheduling
public class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final AuditRunRepository auditRunRepository;

    public RetentionScheduler(AuditRunRepository auditRunRepository) {
        this.auditRunRepository = auditRunRepository;
    }

    /**
     * Purge expired audit runs daily at 2:00 AM UTC.
     * Cascade delete removes associated {@code audit_findings} automatically.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void purgeExpiredRuns() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC);
        int deleted = auditRunRepository.deleteByExpiresAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Retention: purged {} expired audit run(s) (cutoff={})", deleted, cutoff);
        } else {
            log.debug("Retention: no expired audit runs to purge (cutoff={})", cutoff);
        }
    }
}
