package org.baanet.baaapp.api;

import okhttp3.OkHttpClient;
import okhttp3.Dispatcher;

public class ApiClient {

    private static final OkHttpClient client = new OkHttpClient();
    // Keep logout cancellation separate while sharing the connection pool and HTTP settings.
    private static final OkHttpClient syncClient = client.newBuilder()
            .dispatcher(new Dispatcher())
            .build();

    public static OkHttpClient getClient() {
        return client;
    }

    public static OkHttpClient getSyncClient() {
        return syncClient;
    }
}
