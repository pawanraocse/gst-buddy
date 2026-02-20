package com.learning.backendservice.scheduler;

import com.learning.backendservice.repository.Rule37RunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetentionScheduler — daily cleanup of expired runs")
class RetentionSchedulerTest {

    @Mock
    private Rule37RunRepository runRepository;

    private RetentionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RetentionScheduler(runRepository);
    }

    @Test
    @DisplayName("Should delete expired runs when they exist")
    void shouldDeleteExpiredRuns() {
        when(runRepository.deleteByExpiresAtBefore(any(OffsetDateTime.class))).thenReturn(5);

        scheduler.purgeExpiredRuns();

        ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(runRepository).deleteByExpiresAtBefore(cutoffCaptor.capture());

        OffsetDateTime cutoff = cutoffCaptor.getValue();
        assertTrue(cutoff.isBefore(OffsetDateTime.now().plusSeconds(1)),
                "Cutoff should be approximately now");
        assertTrue(cutoff.isAfter(OffsetDateTime.now().minusMinutes(1)),
                "Cutoff should be recent");
    }

    @Test
    @DisplayName("Should complete silently when no expired runs exist")
    void shouldHandleNoExpiredRuns() {
        when(runRepository.deleteByExpiresAtBefore(any(OffsetDateTime.class))).thenReturn(0);

        assertDoesNotThrow(() -> scheduler.purgeExpiredRuns());
        verify(runRepository).deleteByExpiresAtBefore(any(OffsetDateTime.class));
    }
}
