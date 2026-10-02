package org.baanet.baaapp.connection;

final class SyncExecutionState {
    enum RequestResult { STARTED, SKIPPED, DEFERRED }

    private boolean syncing;
    private String pendingPostToken;

    synchronized RequestResult request(boolean post, String token) {
        if (syncing) {
            if (post) {
                pendingPostToken = token;
                return RequestResult.DEFERRED;
            }
            return RequestResult.SKIPPED;
        }
        syncing = true;
        return RequestResult.STARTED;
    }

    synchronized boolean isSyncing() {
        return syncing;
    }

    synchronized String finish() {
        syncing = false;
        String token = pendingPostToken;
        pendingPostToken = null;
        return token;
    }
}
