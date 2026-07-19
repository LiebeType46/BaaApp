package org.baanet.baaapp.api;

import okhttp3.OkHttpClient;

public class ApiClient {

    private static final OkHttpClient client = new OkHttpClient();

    public static OkHttpClient getClient() {
        return client;
    }
}