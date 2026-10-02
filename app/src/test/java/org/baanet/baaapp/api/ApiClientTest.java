package org.baanet.baaapp.api;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.junit.Assert.*;

public class ApiClientTest {
    @Test
    public void logoutCancellationDoesNotCancelSyncCalls() throws Exception {
        assertNotSame(ApiClient.getClient().dispatcher(), ApiClient.getSyncClient().dispatcher());
        assertSame(ApiClient.getClient().connectionPool(), ApiClient.getSyncClient().connectionPool());
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(2);
        Interceptor interceptor = chain -> {
            started.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body(ResponseBody.create("", null)).build();
        };
        OkHttpClient authClient = ApiClient.getClient().newBuilder().addInterceptor(interceptor).build();
        OkHttpClient syncClient = ApiClient.getSyncClient().newBuilder().addInterceptor(interceptor).build();
        Request request = new Request.Builder().url("https://example.invalid/").build();
        Call authCall = authClient.newCall(request);
        Call syncCall = syncClient.newCall(request);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { completed.countDown(); }

            @Override
            public void onResponse(Call call, Response response) {
                response.close();
                completed.countDown();
            }
        };
        authCall.enqueue(callback);
        syncCall.enqueue(callback);
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            ApiClient.getClient().dispatcher().cancelAll();
            assertTrue(authCall.isCanceled());
            assertFalse(syncCall.isCanceled());
        } finally {
            release.countDown();
            assertTrue(completed.await(5, TimeUnit.SECONDS));
        }
    }
}
