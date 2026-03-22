package com.example.baapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.baapp.data.LocationEntity;
import com.example.baapp.location.LocationService;
import com.example.baapp.photo.PhotoService;

import java.util.Locale;

public class LocationDetailActivity extends AppCompatActivity {

    private LocationService locationService;

    private TextView tvCategorySubCategory;
    private TextView tvLocationTimestamp;
    private TextView tvMemo;
    private ImageView ivPhoto;
    private TextView tvOpenMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);

        locationService = LocationService.getInstance(this);

        tvCategorySubCategory = findViewById(R.id.tvCategorySubCategory);
        tvLocationTimestamp = findViewById(R.id.tvLocationTimestamp);
        tvMemo = findViewById(R.id.tvMemo);
        ivPhoto = findViewById(R.id.ivPhoto);
        tvOpenMap = findViewById(R.id.tvOpenMap);

        int locationId = getIntent().getIntExtra("location_id", -1);
        if (locationId == -1) {
            finish();
            return;
        }

        LocationEntity item = locationService.getLocationById(locationId);
        if (item == null) {
            finish();
            return;
        }

        tvCategorySubCategory.setText(item.getCategory() + " / " + item.getSubCategory());
        tvLocationTimestamp.setText(String.format(
                Locale.getDefault(),
                "%.6f, %.6f / %s",
                item.getLatitude(),
                item.getLongitude(),
                item.getTimestamp()
        ));
        tvMemo.setText(item.getMemo() != null ? item.getMemo() : "");

        String photoUriStr = item.getPhotoUri();

        if (photoUriStr != null && !photoUriStr.isEmpty()) {
            Uri photoUri = Uri.parse(photoUriStr);

            ivPhoto.setVisibility(View.VISIBLE);
            ivPhoto.setImageURI(photoUri);

            ivPhoto.setOnClickListener(v ->
                    PhotoService.showFullSizePhoto(LocationDetailActivity.this, photoUri.toString())
            );
        } else {
            ivPhoto.setVisibility(View.GONE);
        }

        tvOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(LocationDetailActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_FOCUS_LOCATION_ID, item.getId());
            startActivity(intent);
        });
    }
}