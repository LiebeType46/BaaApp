package org.baanet.baaapp.connection;

import org.junit.Test;

import static org.junit.Assert.*;

public class SyncScheduleTest {
    @Test
    public void manualOrEventSyncResetsNextScheduledTime() {
        long now = 1_000_000L;
        assertEquals(120_000L, SyncSchedule.remainingDelay(now, now - 180_000L, 5));
        assertEquals(300_000L, SyncSchedule.remainingDelay(now, now, 5));
    }

    @Test
    public void intervalChangeUsesSameLastStartTime() {
        long now = 1_000_000L;
        long lastStart = now - 120_000L;
        assertEquals(0L, SyncSchedule.remainingDelay(now, lastStart, 1));
        assertEquals(480_000L, SyncSchedule.remainingDelay(now, lastStart, 10));
        assertEquals(3_480_000L, SyncSchedule.remainingDelay(now, lastStart, 60));
    }

    @Test
    public void overdueOrMissingStartTimeDoesNotDelaySync() {
        assertEquals(0L, SyncSchedule.remainingDelay(1_000_000L, 700_000L, 5));
        assertEquals(0L, SyncSchedule.remainingDelay(1_000_000L, 0L, 5));
        assertEquals(0L, SyncSchedule.remainingDelay(1_000_000L, 2_000_000L, 5));
    }

    @Test
    public void optionsMatchRequestedIntervalsAndCannotBeMutated() {
        assertArrayEquals(new int[]{0, 1, 5, 10, 20, 30, 40, 50, 60}, SyncSchedule.getIntervalOptions());
        assertEquals(5, SyncSchedule.DEFAULT_INTERVAL_MINUTES);
        assertTrue(SyncSchedule.isSupportedInterval(0));
        assertFalse(SyncSchedule.isSupportedInterval(2));
        assertFalse(SyncSchedule.isSupportedInterval(70));
        int[] options = SyncSchedule.getIntervalOptions();
        options[0] = 99;
        assertEquals(0, SyncSchedule.getIntervalOptions()[0]);
    }
}
