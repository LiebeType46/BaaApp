package com.example.baapp.map;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.util.GeoPoint;

public class MapHelper {

    private final MapView mapView;

    public MapHelper(MapView mapView) {
        this.mapView = mapView;
    }

    public void setCenter(GeoPoint point, double zoomLevel) {
        if (point == null) {
            return;
        }
        mapView.getController().setZoom(zoomLevel);
        mapView.getController().setCenter(point);
        mapView.invalidate();
    }

    public void addMarker(GeoPoint point, String title) {
        if (point == null) {
            return;
        }
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }
}
