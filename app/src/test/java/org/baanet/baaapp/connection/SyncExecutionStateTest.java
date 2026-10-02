package org.baanet.baaapp.connection;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SyncExecutionStateTest {
    @Test
    public void simultaneousRequestsStartOnlyOneSync() throws InterruptedException {
        SyncExecutionState state = new SyncExecutionState();
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger started = new AtomicInteger();
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (state.request(false, "token") == SyncExecutionState.RequestResult.STARTED) {
                        started.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        ready.await();
        start.countDown();
        for (Thread thread : threads) thread.join();
        assertEquals(1, started.get());
    }

    @Test
    public void manualAndPeriodicRequestsShareGuard() {
        SyncExecutionState state = new SyncExecutionState();
        assertEquals(SyncExecutionState.RequestResult.STARTED, state.request(false, "token"));
        assertEquals(SyncExecutionState.RequestResult.SKIPPED, state.request(false, "token"));
        assertTrue(state.isSyncing());
        assertNull(state.finish());
        assertFalse(state.isSyncing());
        assertEquals(SyncExecutionState.RequestResult.STARTED, state.request(false, "token"));
    }

    @Test
    public void multiplePostsBecomeOneFollowUp() {
        SyncExecutionState state = new SyncExecutionState();
        state.request(false, "token");
        assertEquals(SyncExecutionState.RequestResult.DEFERRED, state.request(true, "token"));
        assertEquals(SyncExecutionState.RequestResult.DEFERRED, state.request(true, "token"));
        assertEquals("token", state.finish());
        assertNull(state.finish());
    }

    @Test
    public void latestPostRetainsItsSession() {
        SyncExecutionState state = new SyncExecutionState();
        state.request(false, "old");
        state.request(true, "old");
        state.request(true, "new");
        assertEquals("new", state.finish());
        assertEquals(SyncExecutionState.RequestResult.STARTED, state.request(true, "new"));
    }
}
