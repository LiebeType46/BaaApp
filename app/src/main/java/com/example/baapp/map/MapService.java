package com.example.baapp.map;

import com.example.baapp.data.LocationEntity;
import com.example.baapp.marker.MarkerManager;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MapService {

    private final MapHelper mapHelper;

    public MapService(MapView mapView) {
        mapHelper = new MapHelper(mapView);
    }

    public void updateMapCenter(GeoPoint point, double zoomLevel) {
        mapHelper.setCenter(point, zoomLevel);
    }

    public void addLocationMarker(GeoPoint point, String title) {
        mapHelper.addMarker(point, title);
    }

    public void focusOnLocation(LocationEntity entity, MarkerManager markerManager) {
        if (entity == null) {
            return;
        }

        GeoPoint point = new GeoPoint(entity.getLatitude(), entity.getLongitude());
        updateMapCenter(point, 17.0);

        if (markerManager != null) {
            markerManager.showFocusedLocation(entity);
        }
    }
}
