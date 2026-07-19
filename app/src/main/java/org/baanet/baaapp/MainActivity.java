package org.baanet.baaapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.baanet.baaapp.Csv.CsvImporter;
import org.baanet.baaapp.common.ConstCode;
import org.baanet.baaapp.common.LanguageService;
import org.baanet.baaapp.data.AppDatabase;
import org.baanet.baaapp.data.LocationEntity;
import org.baanet.baaapp.data.SearchConditionEntity;
import org.baanet.baaapp.location.LocationPermissionHelper;
import org.baanet.baaapp.location.LocationService;
import org.baanet.baaapp.map.MapService;
import org.baanet.baaapp.map.MapUtils;
import org.baanet.baaapp.marker.MarkerManager;
import org.baanet.baaapp.photo.PhotoCaptureCallback;
import org.baanet.baaapp.photo.PhotoService;
import org.baanet.baaapp.photo.PhotoSession;
import org.baanet.baaapp.search.SearchCondition;
import org.baanet.baaapp.search.SearchConditionMapper;
import org.baanet.baaapp.ui.MenuService;
import org.baanet.baaapp.ui.TimelineAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    public static final String EXTRA_FOCUS_LOCATION_ID = "focus_location_id";
    private static final String PREF = "baa_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PUBLIC_ID = "public_id";

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
        reloadMapMarkers(null);
    }
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabMenu;
    private ImageButton btnCenterOnCurrentLocation;
    private View mapModeContainer;
    private View timelineModeContainer;
    private View settingsModeContainer;
    private FloatingActionButton fabTimelineSearch;
    private FloatingActionButton fabPost;
    private RecyclerView timelineRecyclerView;
    private SwipeRefreshLayout swipeTimeline;
    private TimelineAdapter timelineAdapter;
    private SearchCondition currentSearchCondition = new SearchCondition();
    private SearchCondition defaultSearchCondition = SearchCondition.createDefault();
    private LanguageService language;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, getSharedPreferences("osmdroid", Context.MODE_PRIVATE));

        setContentView(R.layout.activity_main);
        language = LanguageService.get(this);

        mapModeContainer = findViewById(R.id.mapModeContainer);
        timelineModeContainer = findViewById(R.id.timelineModeContainer);
        settingsModeContainer = findViewById(R.id.settingsModeContainer);
        setupSettingsMode();

        fabMenu = findViewById(R.id.fabMenu);
        btnCenterOnCurrentLocation = findViewById(R.id.btnCenterOnCurrentLocation);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabPost = findViewById(R.id.fabPost);
        fabTimelineSearch = findViewById(R.id.fabTimelineSearch);
        fabMenu.setContentDescription(language.t("common.menu"));
        fabPost.setContentDescription(language.t("common.post"));
        fabTimelineSearch.setContentDescription(language.t("common.search"));
        btnCenterOnCurrentLocation.setContentDescription(language.t("main.center_on_current_location"));

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
        loadCurrentSearchCondition();

        // ユーザー情報確認



        // LocationService 初期化
        locationService = LocationService.getInstance(this);

        GeoPoint lastLocation = locationService.getLastKnownLocation(this);
        if (lastLocation != null) {
            lastLocationPoint = lastLocation;
            mapView.post(() -> {
                mapService.updateMapCenter(lastLocation, 15.0);
                reloadMapMarkers(lastLocation);
            });

            Toast.makeText(this, language.t("main.use_last_location"), Toast.LENGTH_LONG).show();
        }

        // 非同期で現在地を取得 → 反映
        locationService.getCurrentLocationAsync(ctx, (GeoPoint location) -> {
            if (location != null) {
                markerManager.updateCurrentLocation(this, location, language.t("main.current_location"));
                // 🔥 中心移動を遅延実行
                if (!isFinishing() && !isDestroyed()) {
                    if (lastLocationPoint != null) {
                        GeoPoint center = (GeoPoint) mapView.getMapCenter();
                        if (MapUtils.isNearCenter(center, lastLocationPoint, 50)) {
                            mapView.getController().setCenter(location);
                        }
                    }
                    lastLocationPoint = location;
                    reloadMapMarkers(location);

                    Toast.makeText(this, language.t("main.get_current_location"), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, language.t("main.error_get_current_location"), Toast.LENGTH_SHORT).show();
            }
        });

        // メニューのセットアップ
        fabMenu = findViewById(R.id.fabMenu);
        fabMenu.setOnClickListener(MenuService.showPopupMenu(this, fabMenu));

        btnCenterOnCurrentLocation.setOnClickListener(v -> {
            GeoPoint point = lastLocationPoint != null
                    ? lastLocationPoint
                    : locationService.getLastKnownLocation(this);

            if (point != null) {
                mapView.getController().setCenter(point);
                Toast.makeText(this, language.t("main.center_on_current_location"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, language.t("main.error_get_current_location"), Toast.LENGTH_SHORT).show();
            }
        });

        fabPost = findViewById(R.id.fabPost);
        fabPost.setOnClickListener(v ->
                org.baanet.baaapp.ui.DialogHelper.showLocationRegistrationDialog(this)
        );
        fabTimelineSearch.setOnClickListener(v ->
                org.baanet.baaapp.ui.DialogHelper.showSearchConditionDialog(this)
        );

        bottomNavigation = findViewById(R.id.bottomNavigation);
        MenuService.setupBottomNavigation(this, bottomNavigation);

        // タイムライン表示のセットアップ
        timelineRecyclerView = findViewById(R.id.timelineRecyclerView);
        swipeTimeline = findViewById(R.id.swipeTimeline);

        timelineAdapter = new TimelineAdapter(item -> {
            Intent intent = new Intent(MainActivity.this, LocationDetailActivity.class);
            intent.putExtra("location_id", item.getId());
            startActivity(intent);
        });
        timelineRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        timelineRecyclerView.setAdapter(timelineAdapter);

        swipeTimeline.setOnRefreshListener(() -> {
            loadTimelinePosts();
            swipeTimeline.setRefreshing(false);
        });

        showMapMode();
        handleFocusLocationIntent(getIntent());
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
                markerManager.updateCurrentLocation(this, point, language.t("main.current_location"));

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
                Toast.makeText(this, language.t("main.permitted_gps"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, language.t("main.denied_gps"), Toast.LENGTH_SHORT).show();
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

            Toast.makeText(this, language.t("csv.complete_import"), Toast.LENGTH_SHORT).show();

            // CSVインポート完了メッセージの後
            GeoPoint lastLocation = locationService.getLastKnownLocation(this);
            reloadMapMarkers(lastLocation);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleFocusLocationIntent(intent);
    }

    public MarkerManager getMarkerManager() {
        return markerManager;
    }

    public void showMapMode() {
        mapModeContainer.setVisibility(View.VISIBLE);
        timelineModeContainer.setVisibility(View.GONE);
        settingsModeContainer.setVisibility(View.GONE);
    }

    public void showTimelineMode() {
        mapModeContainer.setVisibility(View.GONE);
        timelineModeContainer.setVisibility(View.VISIBLE);
        settingsModeContainer.setVisibility(View.GONE);

        loadTimelinePosts();
    }

    private void handleFocusLocationIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        int locationId = intent.getIntExtra(EXTRA_FOCUS_LOCATION_ID, -1);
        if (locationId == -1) {
            return;
        }

        LocationEntity entity = locationService.getLocationById(locationId);
        if (entity == null) {
            return;
        }

        showMapMode();
        mapService.focusOnLocation(entity, markerManager);

        intent.removeExtra(EXTRA_FOCUS_LOCATION_ID);
    }

    public void showSettingsMode() {
        mapModeContainer.setVisibility(View.GONE);
        timelineModeContainer.setVisibility(View.GONE);
        settingsModeContainer.setVisibility(View.VISIBLE);
        showSettingsTop();
    }

    private void setupSettingsMode() {
        TextView title = findViewById(R.id.tvSettingsTitle);
        TextView general = findViewById(R.id.tvGeneralSettings);
        TextView account = findViewById(R.id.tvAccountSettings);
        TextView dataManagement = findViewById(R.id.tvDataManagement);
        TextView generalBack = findViewById(R.id.tvGeneralSettingsBack);
        TextView generalTitle = findViewById(R.id.tvGeneralSettingsTitle);
        TextView languageSettings = findViewById(R.id.tvLanguageSettings);
        Spinner languageSpinner = findViewById(R.id.spLanguageSettings);
        TextView mapTileCacheClear = findViewById(R.id.tvMapTileCacheClear);
        Button mapTileCacheClearButton = findViewById(R.id.btnMapTileCacheClear);
        TextView appInfo = findViewById(R.id.tvAppInfo);
        TextView accountBack = findViewById(R.id.tvAccountSettingsBack);
        TextView accountTitle = findViewById(R.id.tvAccountSettingsTitle);
        TextView userInfo = findViewById(R.id.tvUserInfo);
        TextView userInfoEdit = findViewById(R.id.tvUserInfoEdit);
        TextView userInfoDetails = findViewById(R.id.tvUserInfoDetails);
        TextView logout = findViewById(R.id.tvLogout);
        TextView dataManagementBack = findViewById(R.id.tvDataManagementBack);
        TextView dataManagementTitle = findViewById(R.id.tvDataManagementTitle);
        TextView csvIo = findViewById(R.id.tvCsvIoSettings);
        TextView serverConnection = findViewById(R.id.tvServerConnectionSettings);

        title.setText(language.t("settings.title"));
        general.setText(language.t("settings.general"));
        account.setText(language.t("settings.account"));
        dataManagement.setText(language.t("settings.data_management"));
        generalBack.setText(language.t("settings.back"));
        generalTitle.setText(language.t("settings.general"));
        languageSettings.setText(language.t("settings.language"));
        mapTileCacheClear.setText(language.t("settings.map_tile_cache_clear"));
        mapTileCacheClearButton.setText(language.t("common.clear"));
        appInfo.setText(language.t("settings.app_info"));
        accountBack.setText(language.t("settings.back"));
        accountTitle.setText(language.t("settings.account"));
        userInfo.setText(language.t("settings.user_info"));
        userInfoEdit.setText(language.t("settings.edit"));
        userInfoDetails.setText(buildUserInfoText());
        logout.setText(language.t("settings.logout"));
        dataManagementBack.setText(language.t("settings.back"));
        dataManagementTitle.setText(language.t("settings.data_management"));
        csvIo.setText(language.t("settings.csv_io"));
        serverConnection.setText(language.t("settings.server_communication"));

        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{language.t("settings.language_japanese")}
        );
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);

        findViewById(R.id.rowGeneralSettings).setOnClickListener(v -> showGeneralSettings());
        findViewById(R.id.rowAccountSettings).setOnClickListener(v -> {
            userInfoDetails.setText(buildUserInfoText());
            showAccountSettings();
        });
        findViewById(R.id.rowDataManagement).setOnClickListener(v -> showDataManagementSettings());
        generalBack.setOnClickListener(v -> showSettingsTop());
        accountBack.setOnClickListener(v -> showSettingsTop());
        dataManagementBack.setOnClickListener(v -> showSettingsTop());
        mapTileCacheClearButton.setOnClickListener(v -> showConfirmDialog(
                language.t("settings.map_tile_cache_clear"),
                language.t("settings.confirm_clear_tile_cache")
        ));
        appInfo.setOnClickListener(v -> showAppInfoDialog());
        userInfoEdit.setOnClickListener(v ->
                Toast.makeText(this, language.t("settings.placeholder"), Toast.LENGTH_SHORT).show()
        );
        logout.setOnClickListener(v -> showConfirmDialog(
                language.t("settings.logout"),
                language.t("settings.confirm_logout")
        ));
        csvIo.setOnClickListener(v -> showCsvIoDialog());
        serverConnection.setOnClickListener(v -> showServerCommunicationDialog());
    }

    private void showSettingsTop() {
        findViewById(R.id.settingsTopContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.generalSettingsContainer).setVisibility(View.GONE);
        findViewById(R.id.accountSettingsContainer).setVisibility(View.GONE);
        findViewById(R.id.dataManagementContainer).setVisibility(View.GONE);
    }

    private void showGeneralSettings() {
        findViewById(R.id.settingsTopContainer).setVisibility(View.GONE);
        findViewById(R.id.generalSettingsContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.accountSettingsContainer).setVisibility(View.GONE);
        findViewById(R.id.dataManagementContainer).setVisibility(View.GONE);
    }

    private void showAccountSettings() {
        findViewById(R.id.settingsTopContainer).setVisibility(View.GONE);
        findViewById(R.id.generalSettingsContainer).setVisibility(View.GONE);
        findViewById(R.id.accountSettingsContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.dataManagementContainer).setVisibility(View.GONE);
    }

    private void showDataManagementSettings() {
        findViewById(R.id.settingsTopContainer).setVisibility(View.GONE);
        findViewById(R.id.generalSettingsContainer).setVisibility(View.GONE);
        findViewById(R.id.accountSettingsContainer).setVisibility(View.GONE);
        findViewById(R.id.dataManagementContainer).setVisibility(View.VISIBLE);
    }

    private void showConfirmDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(language.t("settings.ok"), (dialog, which) ->
                        Toast.makeText(this, language.t("settings.placeholder"), Toast.LENGTH_SHORT).show()
                )
                .setNegativeButton(language.t("common.cancel"), null)
                .show();
    }

    private void showAppInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle(language.t("settings.app_info"))
                .setMessage(buildAppInfoText())
                .setPositiveButton(language.t("settings.ok"), null)
                .show();
    }

    private void showCsvIoDialog() {
        new AlertDialog.Builder(this)
                .setTitle(language.t("settings.csv_io_title"))
                .setPositiveButton(language.t("settings.csv_import"), (dialog, which) ->
                        MenuService.startCsvImport(this)
                )
                .setNegativeButton(language.t("settings.csv_export"), (dialog, which) ->
                        Toast.makeText(this, language.t("settings.placeholder"), Toast.LENGTH_SHORT).show()
                )
                .setNeutralButton(language.t("common.cancel"), null)
                .show();
    }

    private void showServerCommunicationDialog() {
        Spinner communicationSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{
                        language.t("settings.communication_manual"),
                        language.t("settings.communication_auto")
                }
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        communicationSpinner.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle(language.t("settings.server_communication_title"))
                .setView(communicationSpinner)
                .setPositiveButton(language.t("settings.sync_execute"), (dialog, which) ->
                        Toast.makeText(this, language.t("settings.placeholder"), Toast.LENGTH_SHORT).show()
                )
                .setNegativeButton(language.t("common.cancel"), null)
                .show();
    }

    private String buildUserInfoText() {
        SharedPreferences prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        String publicId = prefs.getString(KEY_PUBLIC_ID, language.t("settings.not_set"));
        String token = prefs.getString(KEY_TOKEN, null);
        String tokenStatus = token == null || token.isBlank()
                ? language.t("settings.not_set")
                : language.t("login.ok");

        return language.t("settings.user_public_id") + ": " + publicId
                + "\n" + language.t("settings.user_token") + ": " + tokenStatus;
    }

    private String buildAppInfoText() {
        String versionName = "";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return language.t("settings.app_version") + ": " + versionName
                + "\n" + language.t("settings.app_package") + ": " + getPackageName();
    }

    public SearchCondition getCurrentSearchCondition() {
        if (currentSearchCondition == null) {
            currentSearchCondition = new SearchCondition();
        }
        return currentSearchCondition;
    }

    public void applySearchCondition(SearchCondition condition) {
        currentSearchCondition = condition != null ? condition : new SearchCondition();

        if (currentSearchCondition.hasAnyCondition()
                || currentSearchCondition.hasCustomResultLimit()) {
            db.searchConditionDao().save(SearchConditionMapper.toEntity(currentSearchCondition));
        } else {
            db.searchConditionDao().clearCurrent();
        }

        reloadBySearchCondition();
    }

    private void loadCurrentSearchCondition() {
        ensureDefaultSearchCondition();
        currentSearchCondition = SearchConditionMapper.toDto(db.searchConditionDao().getCurrent());
    }

    private void ensureDefaultSearchCondition() {
        defaultSearchCondition = SearchConditionMapper.toDto(db.searchConditionDao().getDefault());
        if (defaultSearchCondition.getResultLimit() == null) {
            defaultSearchCondition = SearchCondition.createDefault();
            db.searchConditionDao().save(
                    SearchConditionMapper.toEntity(
                            defaultSearchCondition,
                            SearchConditionEntity.DEFAULT_ID
                    )
            );
        }
    }

    private SearchCondition getEffectiveSearchCondition() {
        SearchCondition currentCondition = getCurrentSearchCondition();
        if (currentCondition.getResultLimit() == null) {
            currentCondition.setResultLimit(SearchCondition.DEFAULT_RESULT_LIMIT);
        }

        return currentCondition.hasAnyCondition() || currentCondition.hasCustomResultLimit()
                ? currentCondition
                : defaultSearchCondition;
    }

    private void reloadBySearchCondition() {
        reloadMapMarkers(null);
        if (timelineAdapter != null) {
            loadTimelinePosts();
        }
    }

    private void reloadMapMarkers(GeoPoint fallbackCenter) {
        if (locationService == null || markerManager == null) {
            return;
        }

        GeoPoint searchCenter = getMapSearchCenter(fallbackCenter);
        SearchCondition condition = getEffectiveSearchCondition();
        List<LocationEntity> sourceLocations = locationService.getAllLocationsLatestFirst();
        List<LocationEntity> filteredLocations =
                filterLocationsBySearchCondition(sourceLocations, condition, true, searchCenter);
        filteredLocations = limitLocations(filteredLocations, condition.getResultLimit());

        markerManager.removeMarkers(displayedMarkers);
        markerManager.initMarkers(filteredLocations);
        displayedMarkers = filteredLocations;
    }

    private GeoPoint getMapSearchCenter(GeoPoint fallbackCenter) {
        if (mapView != null) {
            IGeoPoint center = mapView.getMapCenter();
            if (center != null) {
                return new GeoPoint(center.getLatitude(), center.getLongitude());
            }
        }

        return fallbackCenter != null ? fallbackCenter : lastLocationPoint;
    }

    private void loadTimelinePosts() {
        SearchCondition condition = getEffectiveSearchCondition();
        List<LocationEntity> sourcePosts = locationService.getAllLocationsLatestFirst();
        List<LocationEntity> filteredPosts =
                filterLocationsBySearchCondition(sourcePosts, condition, false, null);
        filteredPosts = limitLocations(filteredPosts, condition.getResultLimit());

        timelineAdapter.setItems(filteredPosts);
    }

    private List<LocationEntity> filterLocationsBySearchCondition(
            List<LocationEntity> locations,
            SearchCondition condition,
            boolean includeRadius,
            GeoPoint radiusCenter
    ) {
        List<LocationEntity> filtered = new ArrayList<>();
        Date fromDate = parseTimestamp(condition.getFromTimestamp());
        Date toDate = parseTimestamp(condition.getToTimestamp());

        if (locations == null) {
            return filtered;
        }

        for (LocationEntity entity : locations) {
            if (matchesSearchCondition(entity, condition, fromDate, toDate, includeRadius, radiusCenter)) {
                filtered.add(entity);
            }
        }

        return filtered;
    }

    private List<LocationEntity> limitLocations(List<LocationEntity> locations, Integer resultLimit) {
        if (locations == null || resultLimit == null || locations.size() <= resultLimit) {
            return locations;
        }

        return new ArrayList<>(locations.subList(0, resultLimit));
    }

    private boolean matchesSearchCondition(
            LocationEntity entity,
            SearchCondition condition,
            Date fromDate,
            Date toDate,
            boolean includeRadius,
            GeoPoint radiusCenter
    ) {
        if (entity == null) {
            return false;
        }

        if (!condition.getCategories().isEmpty()
                && !condition.getCategories().contains(entity.getCategory())) {
            return false;
        }

        if (condition.getSubCategory() != null
                && !containsText(entity.getSubCategory(), condition.getSubCategory())) {
            return false;
        }

        if (fromDate != null || toDate != null) {
            Date entityDate = parseTimestamp(entity.getTimestamp());
            if (entityDate == null) {
                return false;
            }
            if (fromDate != null && entityDate.before(fromDate)) {
                return false;
            }
            if (toDate != null && entityDate.after(toDate)) {
                return false;
            }
        }

        if (condition.getMemoKeyword() != null
                && !containsText(entity.getMemo(), condition.getMemoKeyword())) {
            return false;
        }

        if (Boolean.TRUE.equals(condition.getHasPhoto()) && !hasPhoto(entity)) {
            return false;
        }

        if (condition.getUploadFlg() != null
                && condition.getUploadFlg() != entity.isUploadFlg()) {
            return false;
        }

        if (includeRadius && condition.getRadiusMeters() != null && radiusCenter != null) {
            double distance = distanceBetween(radiusCenter, entity);
            if (distance > condition.getRadiusMeters()) {
                return false;
            }
        }

        return true;
    }

    private boolean containsText(String value, String keyword) {
        String normalizedValue = normalizeSearchText(value);
        String normalizedKeyword = normalizeSearchText(keyword);

        if (normalizedKeyword == null) {
            return true;
        }
        if (normalizedValue == null) {
            return false;
        }

        return normalizedValue.toLowerCase(Locale.ROOT)
                .contains(normalizedKeyword.toLowerCase(Locale.ROOT));
    }

    private boolean hasPhoto(LocationEntity entity) {
        String photoUri = normalizeSearchText(entity.getPhotoUri());
        return photoUri != null;
    }

    private Date parseTimestamp(String value) {
        String normalized = normalizeSearchText(value);
        if (normalized == null) {
            return null;
        }

        String[] patterns = {
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
                format.setLenient(false);
                return format.parse(normalized);
            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    private double distanceBetween(GeoPoint point, LocationEntity entity) {
        float[] results = new float[1];
        Location.distanceBetween(
                point.getLatitude(),
                point.getLongitude(),
                entity.getLatitude(),
                entity.getLongitude(),
                results
        );
        return results[0];
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
