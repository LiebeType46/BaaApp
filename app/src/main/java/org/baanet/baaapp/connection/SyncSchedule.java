package org.baanet.baaapp.connection;

final class SyncSchedule {
    static final int DEFAULT_INTERVAL_MINUTES = 5;
    private static final int[] INTERVALS = {0, 1, 5, 10, 20, 30, 40, 50, 60};

    private SyncSchedule() {
    }

    static int[] getIntervalOptions() {
        return INTERVALS.clone();
    }

    static boolean isSupportedInterval(int minutes) {
        for (int option : INTERVALS) {
            if (option == minutes) return true;
        }
        return false;
    }

    static long remainingDelay(long now, long lastSyncAt, int minutes) {
        if (minutes <= 0 || lastSyncAt <= 0 || now < lastSyncAt) return 0;
        return Math.max(0, minutes * 60_000L - (now - lastSyncAt));
    }
}
