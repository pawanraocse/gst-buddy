package com.learning.backendservice.scheduler;

import com.learning.backendservice.repository.Rule37RunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Purges expired Rule 37 calculation runs daily.
 * Runs are marked with {@code expires_at} (default 7 days after creation).
 * This scheduler removes them to prevent unbounded storage growth.
 */
@Component
@EnableScheduling
public class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final Rule37RunRepository runRepository;

    public RetentionScheduler(Rule37RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    /**
     * Purge expired calculation runs daily at 2:00 AM.
     * Uses the {@code idx_rule37_runs_expires} index for efficient lookups.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredRuns() {
        OffsetDateTime now = OffsetDateTime.now();
        int deleted = runRepository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            log.info("Retention: purged {} expired calculation runs (cutoff={})", deleted, now);
        } else {
            log.debug("Retention: no expired runs to purge");
        }
    }
}
