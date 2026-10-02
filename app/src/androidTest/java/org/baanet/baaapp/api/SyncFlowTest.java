package org.baanet.baaapp.api;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.baanet.baaapp.common.LanguageService;
import org.baanet.baaapp.connection.AutoSyncService;
import org.baanet.baaapp.connection.SvConnectService;
import org.baanet.baaapp.data.AppDatabase;
import org.baanet.baaapp.data.LocationEntity;
import org.baanet.baaapp.sync.LocationSyncRequest;
import org.baanet.baaapp.sync.LocationUploadRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SyncFlowTest {
    private Context context;
    private SharedPreferences prefs;
    private AppDatabase db;
    private OkHttpClient originalClient;
    private final CountDownLatch firstRequest = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final List<String> authorizations = Collections.synchronizedList(new ArrayList<>());
    private final List<List<Integer>> batches = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean failFirst;
    private volatile int photoRequests;

    @Before
    public void setUp() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Use separate names and a private test subdirectory inside the process's writable sandbox.
        context = new ContextWrapper(target) {
            @Override
            public Context getApplicationContext() { return this; }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("sync-flow-test-" + name, mode);
            }

            @Override
            public File getDatabasePath(String name) {
                return super.getDatabasePath("sync-flow-test-" + name);
            }

            @Override
            public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
                return super.openOrCreateDatabase("sync-flow-test-" + name, mode, factory);
            }

            @Override
            public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory,
                                                       DatabaseErrorHandler handler) {
                return super.openOrCreateDatabase("sync-flow-test-" + name, mode, factory, handler);
            }

            @Override
            public File getFilesDir() {
                File directory = new File(super.getFilesDir(), "sync-flow-test");
                if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Test directory");
                return directory;
            }
        };
        assertNotEquals(target.getDatabasePath("locations.db"), context.getDatabasePath("locations.db"));
        assertNotEquals(target.getFilesDir(), context.getFilesDir());
        assertSame(context, context.getApplicationContext());
        LanguageService.get(target);
        prefs = context.getSharedPreferences("baa_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().putString("token", "token-A").putString("public_id", "owner-A")
                .putInt("auto_sync_interval_minutes", 1).commit();
        db = AppDatabase.getInstance(context);
        db.locationDao().deleteAll();
        originalClient = ApiClient.getSyncClient();
        ApiClient.setSyncClientForTesting(originalClient.newBuilder().addInterceptor(chain -> {
            authorizations.add(chain.request().header("Authorization"));
            String body;
            if (chain.request().url().encodedPath().endsWith("/sync")) {
                Buffer buffer = new Buffer();
                chain.request().body().writeTo(buffer);
                LocationSyncRequest request = new Gson().fromJson(buffer.readUtf8(), LocationSyncRequest.class);
                List<Integer> ids = new ArrayList<>();
                StringBuilder uploaded = new StringBuilder();
                for (LocationUploadRequest location : request.locations) {
                    ids.add(location.localId);
                    if (uploaded.length() > 0) uploaded.append(',');
                    uploaded.append("{\"localId\":").append(location.localId)
                            .append(",\"serverLocationId\":").append(1000L + location.localId).append('}');
                }
                batches.add(ids);
                if (batches.size() == 1) {
                    firstRequest.countDown();
                    try {
                        if (!release.await(15, TimeUnit.SECONDS)) throw new IOException("Test response timed out");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException(e);
                    }
                    if (failFirst) throw new IOException("Simulated offline failure");
                }
                body = "{\"resCode\":\"OK\",\"uploadedLocations\":[" + uploaded + "]}";
            } else {
                photoRequests++;
                body = "{\"resCode\":\"OK\"}";
            }
            // Return without chain.proceed(): no production-server access.
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body(ResponseBody.create(body, null)).build();
        }).build());
    }

    @After
    public void tearDown() throws Exception {
        release.countDown();
        if (originalClient != null) {
            waitUntil(() -> ApiClient.getSyncClient().dispatcher().runningCallsCount() == 0);
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        AutoSyncService.stopForegroundSync();
        if (originalClient != null) ApiClient.setSyncClientForTesting(originalClient);
        if (db != null) db.locationDao().deleteAll();
        if (prefs != null) prefs.edit().clear().commit();
        new File(context.getFilesDir(), "photos/sync-test.jpg").delete();
    }

    @Test
    public void manualSyncIsSkippedWhilePeriodicSyncIsRunning() throws Exception {
        addLocation(1, "owner-A", false);
        AutoSyncService.requestStartupSync(context);
        assertTrue(firstRequest.await(5, TimeUnit.SECONDS));
        long startedAt = AutoSyncService.getLastSyncAt(context);
        Result duplicate = new Result();
        SvConnectService.upload(context, duplicate);
        assertTrue(duplicate.done.await(5, TimeUnit.SECONDS));
        assertEquals(LanguageService.get(context).t("sync.already_syncing"), duplicate.error);
        assertEquals(1, batches.size());
        assertEquals(startedAt, AutoSyncService.getLastSyncAt(context));
        release.countDown();
        waitUntil(() -> db.locationDao().getUnuploadedLocationsByOwner("owner-A").isEmpty());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Result after = new Result();
        SvConnectService.upload(context, after);
        assertTrue(after.done.await(5, TimeUnit.SECONDS));
        assertTrue(after.noTarget);
    }

    @Test
    public void overduePeriodicSyncWaitsUntilManualSyncFinishes() throws Exception {
        addLocation(1, "owner-A", false);
        Result result = new Result();
        SvConnectService.upload(context, result);
        assertTrue(firstRequest.await(5, TimeUnit.SECONDS));
        long overdueStart = System.currentTimeMillis() - 61_000L;
        prefs.edit().putLong("last_sync_at", overdueStart).commit();
        AutoSyncService.requestResumeSync(context);
        AutoSyncService.setIntervalMinutes(context, 1);
        assertEquals(overdueStart, AutoSyncService.getLastSyncAt(context));
        assertEquals(1, batches.size());
        release.countDown();
        assertTrue(result.done.await(5, TimeUnit.SECONDS));
        waitUntil(() -> AutoSyncService.getLastSyncAt(context) > overdueStart);
        assertEquals(1, batches.size());
        assertTrue(db.locationDao().getUnuploadedLocationsByOwner("owner-A").isEmpty());
    }

    @Test
    public void postsDuringManualSyncAreUploadedInOneFollowUp() throws Exception {
        addLocation(1, "owner-A", false);
        Result result = new Result();
        SvConnectService.upload(context, result);
        assertTrue(firstRequest.await(5, TimeUnit.SECONDS));
        addLocation(2, "owner-A", false);
        AutoSyncService.requestPostSync(context);
        addLocation(3, "owner-A", false);
        AutoSyncService.requestPostSync(context);
        assertEquals(1, batches.size());
        release.countDown();
        waitUntil(() -> db.locationDao().getUnuploadedLocationsByOwner("owner-A").isEmpty());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(2, batches.size());
        assertEquals(java.util.Arrays.asList(2, 3), batches.get(1));
    }

    @Test
    public void failedSyncLeavesPostsForNextSync() throws Exception {
        failFirst = true;
        addLocation(1, "owner-A", false);
        Result failed = new Result();
        SvConnectService.upload(context, failed);
        assertTrue(firstRequest.await(5, TimeUnit.SECONDS));
        addLocation(2, "owner-A", false);
        AutoSyncService.requestPostSync(context);
        release.countDown();
        assertTrue(failed.done.await(5, TimeUnit.SECONDS));
        assertNotNull(failed.error);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertEquals(1, batches.size());
        assertEquals(2, db.locationDao().getUnuploadedLocationsByOwner("owner-A").size());
        Result retry = new Result();
        SvConnectService.upload(context, retry);
        assertTrue(retry.done.await(5, TimeUnit.SECONDS));
        assertNull(retry.error);
        assertEquals(java.util.Arrays.asList(1, 2), batches.get(1));
    }

    @Test
    public void logoutKeepsStartedSyncButDropsUnstartedFollowUp() throws Exception {
        addLocation(1, "owner-A", true);
        Result result = new Result();
        SvConnectService.upload(context, result);
        assertTrue(firstRequest.await(5, TimeUnit.SECONDS));
        addLocation(2, "owner-A", false);
        AutoSyncService.requestPostSync(context);
        prefs.edit().remove("token").remove("public_id").commit();
        ApiClient.getClient().dispatcher().cancelAll();
        release.countDown();
        assertTrue(result.done.await(5, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertNull(result.error);
        assertEquals(1, photoRequests);
        assertEquals(1, batches.size());
        assertEquals(java.util.Arrays.asList("Bearer token-A", "Bearer token-A"), authorizations);
        assertEquals(1, db.locationDao().getUnuploadedLocationsByOwner("owner-A").size());
    }

    @Test
    public void accountSwitchDoesNotStopOrRetargetStartedPhotoSync() throws Exception {
        addLocation(1, "owner-A", true);
        addLocation(2, "owner-B", false);
        Result result = new Result();
        SvConnectService.upload(context, result);
        assertTrue(firstRequest.await(5, TimeUnit.SECONDS));
        prefs.edit().remove("token").remove("public_id").commit();
        ApiClient.getClient().dispatcher().cancelAll();
        prefs.edit().putString("token", "token-B").putString("public_id", "owner-B").commit();
        release.countDown();
        assertTrue(result.done.await(5, TimeUnit.SECONDS));
        assertNull(result.error);
        assertEquals(1, photoRequests);
        assertEquals(java.util.Arrays.asList("Bearer token-A", "Bearer token-A"), authorizations);
        assertEquals(1, db.locationDao().getUnuploadedLocationsByOwner("owner-B").size());
        assertTrue(db.locationDao().findByIdAndOwner(1, "owner-A").isPhotoUploadFlg());
    }

    private void addLocation(int id, String owner, boolean photo) throws IOException {
        String path = null;
        if (photo) {
            path = "photos/sync-test.jpg";
            File file = new File(context.getFilesDir(), path);
            assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
            try (FileOutputStream output = new FileOutputStream(file)) { output.write(new byte[]{1, 2, 3}); }
        }
        LocationEntity location = new LocationEntity("DAILY", null, 35, 139,
                "2026/10/02 12:00:00", "Automated sync test", false, path);
        location.setId(id);
        location.setOwnerPublicId(owner);
        db.locationDao().insert(location);
    }

    private static void waitUntil(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(20);
        assertTrue("Timed out waiting for sync", condition.getAsBoolean());
    }

    private static class Result implements SvConnectService.UploadCallback {
        final CountDownLatch done = new CountDownLatch(1);
        volatile String error;
        volatile boolean noTarget;

        public void onComplete(int locations, int photos, int failedPhotos) { done.countDown(); }
        public void onNoTarget() { noTarget = true; done.countDown(); }
        public void onError(String message) { error = message; done.countDown(); }
    }
}
