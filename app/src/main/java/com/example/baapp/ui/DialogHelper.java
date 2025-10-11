package com.example.baapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.baapp.MainActivity;
import com.example.baapp.R;
import com.example.baapp.common.MainCategory;
import com.example.baapp.data.LocationEntity;
import com.example.baapp.location.LocationService;
import com.example.baapp.photo.PhotoService;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DialogHelper {

    public static void showLocationRegistrationDialog(Context context) {
        new DialogHelper(context).show();
    }

    private final Context context;
    private final LocationService locationService;
    private final PhotoService photoService;

    private Spinner categorySpinner;
    private EditText latitudeField;
    private EditText longitudeField;
    private EditText timestampField;
    private EditText memoField;

    private ImageView photoPreview;
    private Uri photoUri;
    private File photoFile;

    private DialogHelper(Context context) {
        this.context = context;
        this.locationService = LocationService.getInstance(context);
        this.photoService = PhotoService.getInstance(context);
    }

    private void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_registration, null);
        builder.setView(dialogView);

        categorySpinner = dialogView.findViewById(R.id.categorySpinner);

        // 表示用ラベルリストの作成
        List<String> categoryLabels = new ArrayList<>();
        for (MainCategory category : MainCategory.values()) {
            categoryLabels.add(category.getLabel());
        }

        // Spinnerにアダプタを設定
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,  // ActivityやDialogのコンテキスト
                android.R.layout.simple_spinner_item,
                categoryLabels
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        
        latitudeField = dialogView.findViewById(R.id.latitudeField);
        longitudeField = dialogView.findViewById(R.id.longitudeField);
        timestampField = dialogView.findViewById(R.id.timestampField);
        memoField = dialogView.findViewById(R.id.memoField);

        Button btnAddPhoto = dialogView.findViewById(R.id.btnAddPhoto);
        photoPreview = dialogView.findViewById(R.id.photoPreview);

        btnAddPhoto.setOnClickListener(v -> showPhotoOptionDialog());

        initializeDialogFields();

        builder.setPositiveButton("登録", (dialog, which) -> saveLocation());
        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void initializeDialogFields() {
        GeoPoint lastKnown = locationService.getLastKnownLocation(context);
        if (lastKnown != null) {
            setLocationFields(lastKnown);
        } else {
            locationService.getCurrentLocationAsync(context, location -> {
                if (location != null) {
                    setLocationFields(location);
                } else {
                    latitudeField.setText("取得できませんでした");
                    longitudeField.setText("取得できませんでした");
                    timestampField.setText("");
                }
            });
        }
    }

    private void setLocationFields(GeoPoint location) {
        latitudeField.setText(String.format(Locale.getDefault(), "%.6f", location.getLatitude()));
        longitudeField.setText(String.format(Locale.getDefault(), "%.6f", location.getLongitude()));

        String timestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
        timestampField.setText(timestamp);
    }

    private void saveLocation() {
        String category = categorySpinner.getSelectedItem().toString();
        String latStr = latitudeField.getText().toString();
        String lonStr = longitudeField.getText().toString();
        String timestamp = timestampField.getText().toString();
        String memo = memoField.getText().toString();
        String uriStr = photoUri != null ? photoUri.toString() : "";

        try {
            double latitude = Double.parseDouble(latStr);
            double longitude = Double.parseDouble(lonStr);

            locationService.saveLocation(category, latStr, lonStr, timestamp, memo, uriStr);
            Toast.makeText(context, "登録しました: " + memo, Toast.LENGTH_SHORT).show();

            if (context instanceof MainActivity) {
                GeoPoint lastLocation = locationService.getLastKnownLocation(context);
                List<LocationEntity> recent = locationService.getRecentLocationsSortedByDistance(context, lastLocation);
                ((MainActivity) context).getMarkerManager().initMarkers(recent);
            }


        } catch (NumberFormatException e) {
            Toast.makeText(context, "有効な位置情報を入力してください。", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "登録に失敗しました。", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhotoOptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String[] options = {"写真を撮影", "画像を選択", "キャンセル"};

        builder.setTitle("写真を追加")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            dialog.dismiss();
                            try {
                                photoService.launchCamera((Activity) context, uri -> {
                                    try {
                                        File file = photoService.copyAndResizePhotoFromUri(uri);
                                        if (file != null) {
                                            photoFile = file;
                                            photoUri = Uri.fromFile(file);

                                            Bitmap thumbnail = photoService.generateThumbnail(file);
                                            photoPreview.setVisibility(View.VISIBLE);
                                            photoPreview.setImageBitmap(thumbnail);
                                        } else {
                                            Toast.makeText(context, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(context, "画像の処理中にエラーが発生しました", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (IOException e) {
                                Toast.makeText(context, "カメラ起動に失敗しました", Toast.LENGTH_SHORT).show();
                            }

                            break;
                        case 1:
                            dialog.dismiss();
                            photoService.launchGallery((Activity) context, uri -> {
                                try {
                                    File file = photoService.copyAndResizePhotoFromUri(uri);
                                    if (file != null) {
                                        photoFile = file;
                                        photoUri = Uri.fromFile(file);

                                        Bitmap thumbnail = photoService.generateThumbnail(file);
                                        photoPreview.setVisibility(View.VISIBLE);
                                        photoPreview.setImageBitmap(thumbnail);
                                    } else {
                                        Toast.makeText(context, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(context, "画像の処理中にエラーが発生しました", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;

                        default:
                            dialog.dismiss();
                    }
                }).show();
    }

}
