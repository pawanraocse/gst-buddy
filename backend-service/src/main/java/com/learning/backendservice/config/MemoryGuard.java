package com.learning.backendservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Pre-flight memory estimator for upload requests.
 * <p>
 * Estimates peak memory usage based on file sizes and Apache POI's DOM-based
 * XSSFWorkbook amplification factor (~7x raw file size). Rejects requests that
 * would risk OutOfMemoryError before any parsing begins.
 * </p>
 *
 * @see com.learning.backendservice.service.LedgerUploadOrchestrator
 */
@Component
public class MemoryGuard {

    private static final Logger log = LoggerFactory.getLogger(MemoryGuard.class);

    /**
     * Apache POI XSSFWorkbook (DOM-based) memory amplification factor.
     * A 10MB .xlsx decompresses into ~70MB of in-memory DOM objects.
     * Conservative estimate; actual amplification is 5-10x depending on cell count and styles.
     */
    private static final int POI_MEMORY_MULTIPLIER = 7;

    /**
     * Reserve 30% of available heap for concurrent operations (DB queries,
     * JSON serialization, GC overhead, other request threads).
     */
    private static final double HEADROOM_FACTOR = 0.30;

    /**
     * Estimates peak memory usage for the given files and rejects
     * if it would exceed the safe memory budget.
     *
     * @param files list of uploaded files to estimate
     * @throws IllegalArgumentException if estimated memory exceeds safe budget
     */
    public void checkMemoryBudget(List<MultipartFile> files) {
        long totalBytes = files.stream()
                .mapToLong(MultipartFile::getSize)
                .sum();

        long estimatedPeakMB = (totalBytes * POI_MEMORY_MULTIPLIER) / (1024 * 1024);

        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory();
        long usedMem = rt.totalMemory() - rt.freeMemory();
        long freeMem = maxMem - usedMem;
        long freeMemMB = freeMem / (1024 * 1024);
        long safeMemMB = (long) (freeMemMB * (1 - HEADROOM_FACTOR));

        log.debug("MemoryGuard: totalFileSize={}MB, estimatedPeak={}MB, freeHeap={}MB, safeBudget={}MB",
                totalBytes / (1024 * 1024), estimatedPeakMB, freeMemMB, safeMemMB);

        if (estimatedPeakMB > safeMemMB) {
            log.warn("MemoryGuard REJECTED: estimatedPeak={}MB exceeds safeBudget={}MB "
                            + "(freeHeap={}MB, maxHeap={}MB, fileCount={}, totalSize={}MB)",
                    estimatedPeakMB, safeMemMB, freeMemMB,
                    maxMem / (1024 * 1024), files.size(), totalBytes / (1024 * 1024));

            throw new IllegalArgumentException(
                    "Upload too large for current server capacity. "
                            + "Estimated memory: " + estimatedPeakMB + "MB, "
                            + "available: " + safeMemMB + "MB. "
                            + "Please upload fewer or smaller files per request.");
        }
    }
}
