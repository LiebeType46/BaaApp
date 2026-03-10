package com.example.baapp.marker;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.example.baapp.R;
import com.example.baapp.data.LocationEntity;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.List;

public class MarkerManager {

    private final MapView mapView;
    private Marker currentLocationMarker; // ← 現在地用マーカーを1つだけ保持

    private final OnThumbnailClickListener thumbnailClickListener;

    private MapEventsOverlay mapEventsOverlay;

    public MarkerManager(MapView mapView, OnThumbnailClickListener listener) {
        this.mapView = mapView;
        this.thumbnailClickListener = listener;
    }

    public interface OnThumbnailClickListener {
        void onClick(String photoUri);
    }

    public void addMarker(LocationEntity entity) {
        if (entity == null){
            return;
        }
        Marker marker = new Marker(mapView);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setPosition(new GeoPoint(entity.getLatitude(), entity.getLongitude()));
        marker.setTitle(entity.getCategory() + entity.getMemo());

        marker.setRelatedObject(entity);

        marker.setInfoWindow(new BasicInfoWindow(
                R.layout.marker_info_window,
                mapView,
                thumbnailClickListener
                ));

        // ✅ 他の InfoWindow を閉じてから自分を表示する
        marker.setOnMarkerClickListener((m, v) -> {
            InfoWindow.closeAllInfoWindowsOn(mapView);  // 他を閉じる
            m.showInfoWindow();                         // 自分だけ開く
            return true;
        });

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }
    // 現在地マーカーの更新用（1つだけ）
    public void updateCurrentLocation(Context context, GeoPoint location, String title) {
        if (currentLocationMarker == null) {
            currentLocationMarker = new Marker(mapView);
            currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(currentLocationMarker);
        }

        currentLocationMarker.setPosition(location);
        Drawable icon = context.getResources().getDrawable(R.drawable.current_location_marker);
        currentLocationMarker.setIcon(icon);
        currentLocationMarker.setTitle(title);
        mapView.invalidate();
    }

    public void initMarkers(List<LocationEntity> locationList) {
        for (LocationEntity entity : locationList) {
            addMarker(entity);
        }
        setupMapClickListener();
    }

    // 登録済みのマーカーを一括で削除（現在地マーカーは除外）
    public void removeMarkers(List<LocationEntity> markerList) {
        if (markerList == null) return;

        for (LocationEntity entity : markerList) {
            GeoPoint target = new GeoPoint(entity.getLatitude(), entity.getLongitude());

            // マーカーを検索して一致するものを削除
            mapView.getOverlays().removeIf(overlay ->
                    overlay instanceof Marker
                            && !(overlay == currentLocationMarker)
                            && ((Marker) overlay).getPosition().equals(target)
            );
        }

        mapView.invalidate();
    }

    // マネージャ初期化時に呼び出す（または mapView セット直後）
    public void setupMapClickListener() {
        if (mapEventsOverlay != null) {
            return;
        }

        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                InfoWindow.closeAllInfoWindowsOn(mapView);
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        mapEventsOverlay = new MapEventsOverlay(receiver);
        mapView.getOverlays().add(mapEventsOverlay);
    }

}
