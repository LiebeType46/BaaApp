package com.example.baapp.marker;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.baapp.R;
import com.example.baapp.data.LocationEntity;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.Locale;

public class BasicInfoWindow extends InfoWindow {

    private final MarkerManager.OnThumbnailClickListener listener;

    public BasicInfoWindow(int layoutResId, MapView mapView, MarkerManager.OnThumbnailClickListener listener) {
        super(layoutResId, mapView);
        this.listener = listener;
    }

    @Override
    public void onOpen(Object item) {
        Marker marker = (Marker) item;

        TextView categoryView = mView.findViewById(R.id.info_category);
        TextView latlonView = mView.findViewById(R.id.info_latlon);
        TextView timeView = mView.findViewById(R.id.info_time);
        TextView memoView = mView.findViewById(R.id.info_memo);
        ImageView imageView = mView.findViewById(R.id.info_image);

        if (marker.getRelatedObject() instanceof LocationEntity) {
            LocationEntity entity = (LocationEntity) marker.getRelatedObject();
            categoryView.setText(entity.getCategory());
            latlonView.setText(String.format(Locale.getDefault(), "%.6f, %.6f", entity.getLatitude(), entity.getLongitude()));
            timeView.setText(entity.getTimestamp());
            memoView.setText(entity.getMemo());

            if (entity.getPhotoUri() != null && !entity.getPhotoUri().isEmpty()) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageURI(Uri.parse(entity.getPhotoUri()));
                imageView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onClick(entity.getPhotoUri());
                    }
                });

            } else {
                imageView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClose() {
        // クリーンアップ不要なら空でOK
    }
}
