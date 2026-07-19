package org.baanet.baaapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.baanet.baaapp.common.CategoryLabelResolver;
import org.baanet.baaapp.common.MainCategoryConverter;
import org.baanet.baaapp.data.LocationEntity;
import org.baanet.baaapp.common.LanguageService;
import org.baanet.baaapp.location.LocationService;
import org.baanet.baaapp.photo.PhotoService;

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
        LanguageService language = LanguageService.get(this);
        ivPhoto.setContentDescription(language.t("photo.image_description"));
        tvOpenMap.setText(language.t("detail.open_map"));

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

        String categoryLabel = CategoryLabelResolver.getLabel(
                this,
                MainCategoryConverter.toCategory(item.getCategory())
        );
        String subCategory = item.getSubCategory() != null ? item.getSubCategory() : "";
        tvCategorySubCategory.setText(categoryLabel + " / " + subCategory);
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
            Uri photoUri = PhotoService.getInstance(this).resolvePhotoUri(photoUriStr);

            ivPhoto.setVisibility(View.VISIBLE);
            ivPhoto.setImageURI(photoUri);

            ivPhoto.setOnClickListener(v ->
                    PhotoService.showFullSizePhoto(LocationDetailActivity.this, photoUriStr)
            );
        } else {
            ivPhoto.setVisibility(View.GONE);
        }

        tvOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(LocationDetailActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(MainActivity.EXTRA_FOCUS_LOCATION_ID, item.getId());
            startActivity(intent);
        });
    }
}
