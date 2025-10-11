package com.example.baapp.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.baapp.data.AppDatabase;
import com.example.baapp.data.LocationEntity;
import com.example.baapp.photo.PhotoService;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LocationService {

    private final LocationManager locationManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long TIMEOUT_MS = 100000; // タイムアウト時間（ミリ秒）
    private final AppDatabase db;
    private static LocationService instance = null;
    private Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private boolean isPolling = false;
    private static final long POLLING_INTERVAL_MS = 10_000;

    private String provider;

    public static synchronized LocationService getInstance(Context context) {
        if (instance == null) {
            instance = new LocationService(context.getApplicationContext());
        }
        return instance;
    }

    public LocationService(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        db = AppDatabase.getInstance(context.getApplicationContext());

        String dbPath = context.getDatabasePath("locations.db").getAbsolutePath();
        Log.d("LocationService", "データベースのフルパス: " + dbPath);

        if (db == null) {
            Log.e("LocationService", "データベースの初期化に失敗しました。");
        } else {
            Log.d("LocationService", "データベースの初期化に成功しました。");
        }
    }

    public void getCurrentLocationAsync(Context context, LocationCallback callback) {
        provider = getBestProvider();
        if (provider == null) {
            callback.onLocationResult(null);
            return;
        }

        try {
            LocationListener listener = new LocationListener() {
                boolean handled = false;

                @Override
                public void onLocationChanged(Location location) {
                    if (!handled) {
                        handled = true;
                        locationManager.removeUpdates(this);
                        callback.onLocationResult(new GeoPoint(location.getLatitude(), location.getLongitude()));
                    }
                }

                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            };

            locationManager.requestLocationUpdates(provider, 0, 0, listener);

            handler.postDelayed(() -> {
                locationManager.removeUpdates(listener);
                Location lastKnown = locationManager.getLastKnownLocation(provider);
                if (lastKnown != null) {
                    callback.onLocationResult(new GeoPoint(lastKnown.getLatitude(), lastKnown.getLongitude()));
                } else {
                    callback.onLocationResult(null);
                }
            }, TIMEOUT_MS);

        } catch (SecurityException e) {
            e.printStackTrace();
            callback.onLocationResult(null);
        }
    }

    @Nullable
    private String getBestProvider() {
        List<String> providers = locationManager.getAllProviders();
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        } else if (providers.contains("fused")) {
            return "fused";
        }
        return null;
    }

    public void saveLocation(String category, String latitude, String longitude, String timestamp, String memo, String photoPath) {
        try {
            LocationEntity locationEntity = createLocationEntity(category, latitude, longitude, timestamp, memo, photoPath);

            db.locationDao().insert(locationEntity);
            Log.d("LocationService", "データが保存されました。");

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("LocationService", "保存に失敗しました。");
        }
    }

    private LocationEntity createLocationEntity(String cat, String latStr, String lonStr, String ts, String memo, String photoPath) {
        double lat = Double.parseDouble(latStr);
        double lon = Double.parseDouble(lonStr);
        return new LocationEntity(cat, lat, lon, ts, memo, photoPath);
    }

    public interface LocationCallback {
        void onLocationResult(@Nullable GeoPoint point);
    }

    public GeoPoint getLastKnownLocation(Context context) {
        provider = getBestProvider();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("LocationService", "位置情報のパーミッションがありません。");
            return null;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            return new GeoPoint(location.getLatitude(), location.getLongitude());
        }
        return null;
    }

    public void startLocationPolling(Context context, PollingCallback callback) {
        if (isPolling) return;

        // ✅ 1回だけ即時取得（getLastKnownLocation）
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            String provider = getBestProvider();
            if (provider != null) {
                Location lastKnown = locationManager.getLastKnownLocation(provider);
                if (lastKnown != null) {
                    callback.onLocationUpdate(new GeoPoint(lastKnown.getLatitude(), lastKnown.getLongitude()));
                }
            }
        }

        // ✅ 位置変化があるたびに通知（requestLocationUpdates）
        LocationListener systemListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                callback.onLocationUpdate(point);
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        };

        try {
            String provider = getBestProvider();
            if (provider != null) {
                locationManager.requestLocationUpdates(provider, POLLING_INTERVAL_MS, 0, systemListener);
                isPolling = true;
                Log.d("LocationService", "ポーリング開始（システム位置更新）");
            }
        } catch (SecurityException e) {
            Log.w("LocationService", "位置情報のパーミッションが不足しています", e);
        }

        // 🔧 既存の lastKnownLocation を定期的に確認したい場合は、この postDelayed を残してもOKです（今はコメントアウト）
    /*
    pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                String provider = getBestProvider();
                if (provider != null) {
                    Location location = locationManager.getLastKnownLocation(provider);
                    if (location != null) {
                        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                        callback.onLocationUpdate(point);
                    }
                }
                pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        }
    };
    pollingHandler.post(pollingRunnable);
    */
    }



    public interface PollingCallback {
        void onLocationUpdate(@Nullable GeoPoint point);
    }


    public void stopLocationPolling() {
        if (isPolling && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            isPolling = false;
            Log.d("LocationService", "ポーリング停止");
        }
    }

    public List<LocationEntity> getRecentLocationsSortedByDistance(Context context, GeoPoint currentLocation) {
        String oneMonthAgo = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000));

        List<LocationEntity> recentLocations = db.locationDao().getLocationsWithinTimeRange(oneMonthAgo);

        if (currentLocation == null) return recentLocations; // fallback

        recentLocations.sort(Comparator.comparingDouble(location ->
                distanceBetween(currentLocation, new GeoPoint(location.getLatitude(), location.getLongitude()))
        ));

        return recentLocations.stream().limit(20).collect(Collectors.toList());
    }

    private double distanceBetween(GeoPoint a, GeoPoint b) {
        float[] result = new float[1];
        Location.distanceBetween(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude(), result);
        return result[0];
    }

}
