package com.example.baapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.baapp.Csv.CsvImporter;
import com.example.baapp.common.ConstCode;
import com.example.baapp.data.AppDatabase;
import com.example.baapp.data.LocationEntity;
import com.example.baapp.location.LocationPermissionHelper;
import com.example.baapp.location.LocationService;
import com.example.baapp.map.MapService;
import com.example.baapp.map.MapUtils;
import com.example.baapp.marker.MarkerManager;
import com.example.baapp.photo.PhotoCaptureCallback;
import com.example.baapp.photo.PhotoService;
import com.example.baapp.photo.PhotoSession;
import com.example.baapp.ui.MenuService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private MapView mapView;
    private MarkerManager markerManager;
    private AppDatabase db;
    private LocationService locationService;
    public LocationService getLocationService() {
        return locationService;
    }
    private MapService mapService;
    private List<LocationEntity> displayedMarkers = new ArrayList<>();
    private GeoPoint lastLocationPoint = null;
    public void refreshMarkers() {
        GeoPoint lastLocation = locationService.getLastKnownLocation(this);
        List<LocationEntity> recent =
                locationService.getRecentLocationsSortedByDistance(this, lastLocation);
        markerManager.initMarkers(recent);
    }
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabMenu;
    private ImageButton btnCenterOnCurrentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, getSharedPreferences("osmdroid", Context.MODE_PRIVATE));

        setContentView(R.layout.activity_main);

        // パーミッション確認
        if (!LocationPermissionHelper.hasLocationPermission(this)) {
            LocationPermissionHelper.requestLocationPermission(this);
        }

        // 地図初期化
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // その他初期化
        markerManager = new MarkerManager(mapView, photoUri -> {
            PhotoService.showFullSizePhoto(MainActivity.this, photoUri);
        });
        mapService = new MapService(mapView);

        db = AppDatabase.getInstance(getApplicationContext());

        // ユーザー情報確認



        // LocationService 初期化
        locationService = LocationService.getInstance(this);

        GeoPoint lastLocation = locationService.getLastKnownLocation(this);
        if (lastLocation != null) {
            mapView.post(() -> mapService.updateMapCenter(lastLocation, 15.0));

            List<LocationEntity> tempLocations = locationService.getRecentLocationsSortedByDistance(this, lastLocation);

            markerManager.initMarkers(tempLocations);

            displayedMarkers = tempLocations;
            lastLocationPoint = lastLocation;

            Toast.makeText(this, R.string.use_last_location, Toast.LENGTH_LONG).show();
        }

        // 非同期で現在地を取得 → 反映
        locationService.getCurrentLocationAsync(ctx, (GeoPoint location) -> {
            if (location != null) {
                markerManager.updateCurrentLocation(this, location, this.getString(R.string.current_location));
                // 🔥 中心移動を遅延実行
                if (!isFinishing() && !isDestroyed()) {
                    markerManager.removeMarkers(displayedMarkers);

                    if (lastLocationPoint != null) {
                        GeoPoint center = (GeoPoint) mapView.getMapCenter();
                        if (MapUtils.isNearCenter(center, lastLocationPoint, 50)) {
                            mapView.getController().setCenter(location);
                        }
                    }
                    lastLocationPoint = location;

                    List<LocationEntity> recentLocations = locationService.getRecentLocationsSortedByDistance(this, location);

                    markerManager.initMarkers(recentLocations);

                    displayedMarkers = recentLocations;

                    Toast.makeText(this, R.string.get_current_location, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.error_get_current_location, Toast.LENGTH_SHORT).show();
            }
        });

        // メニューのセットアップ
        fabMenu = findViewById(R.id.fabMenu);
        fabMenu.setOnClickListener(MenuService.showPopupMenu(this, fabMenu));

        ImageButton centerButton = findViewById(R.id.btnCenterOnCurrentLocation);
        centerButton.setOnClickListener(v -> {
            GeoPoint point = lastLocationPoint != null
                    ? lastLocationPoint
                    : locationService.getLastKnownLocation(this);

            if (point != null) {
                mapView.getController().setCenter(point);
                Toast.makeText(this, R.string.center_on_current_location, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.error_get_current_location, Toast.LENGTH_SHORT).show();
            }
        });

        bottomNavigation = findViewById(R.id.bottomNavigation);
        MenuService.setupBottomNavigation(this, bottomNavigation);

    }


    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        locationService.startLocationPolling(this, point -> {
            if (point != null) {
                markerManager.updateCurrentLocation(this, point, this.getString(R.string.current_location));

                if (lastLocationPoint != null) {
                    GeoPoint center = (GeoPoint) mapView.getMapCenter();
                    if (MapUtils.isNearCenter(center, lastLocationPoint, 50)) {
                        mapView.getController().setCenter(point);
                    }
                }
                lastLocationPoint = point;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        locationService.stopLocationPolling();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppDatabase.getInstance(this).flushToDisk();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppDatabase.getInstance(getApplicationContext()).checkpoint();
        mapView.onDetach();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permitted_gps, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.denied_gps, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ConstCode.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Uri uri = PhotoSession.getInstance().getPhotoUri();
            PhotoCaptureCallback callback = PhotoSession.getInstance().getCallback();

            if (uri != null && callback != null) {
                callback.onPhotoCaptured(uri);
            }

            PhotoSession.getInstance().clear();
        }

        if (requestCode == ConstCode.REQUEST_GALLERY && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            PhotoCaptureCallback callback = PhotoSession.getInstance().getCallback();
            if (uri != null && callback != null) {
                callback.onPhotoCaptured(uri);
            }
        }

        if (requestCode == 1002 && resultCode == RESULT_OK) {
            Uri fileUri = data.getData();
            CsvImporter importer = new CsvImporter(db);
            importer.importFromCsv(this, fileUri);

            Toast.makeText(this, R.string.complete_import_csv, Toast.LENGTH_SHORT).show();

            // CSVインポート完了メッセージの後
            GeoPoint lastLocation = locationService.getLastKnownLocation(this);
            List<LocationEntity> recent = locationService.getRecentLocationsSortedByDistance(this, lastLocation);
            markerManager.initMarkers(recent);
        }
    }

    public MarkerManager getMarkerManager() {
        return markerManager;
    }

    public void showMapMode() {
        if (mapView != null) {
            mapView.setVisibility(View.VISIBLE);
        }
        if (fabMenu != null) {
            fabMenu.setVisibility(View.VISIBLE);
        }
        if (btnCenterOnCurrentLocation != null) {
            btnCenterOnCurrentLocation.setVisibility(View.VISIBLE);
        }
    }

    public void showTimelineMode() {
        if (mapView != null) {
            mapView.setVisibility(View.GONE);
        }
        if (fabMenu != null) {
            fabMenu.setVisibility(View.GONE);
        }
        if (btnCenterOnCurrentLocation != null) {
            btnCenterOnCurrentLocation.setVisibility(View.GONE);
        }

        Toast.makeText(this, "タイムライン表示モード", Toast.LENGTH_SHORT).show();
    }

    public void showAccountOptions() {
        Toast.makeText(this, "アカウントオプション", Toast.LENGTH_SHORT).show();
    }
}
