package com.example.baapp.map;

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
}
