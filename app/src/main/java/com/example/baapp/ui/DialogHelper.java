package com.example.baapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.baapp.MainActivity;
import com.example.baapp.R;
import com.example.baapp.common.MainCategory;
import com.example.baapp.location.LocationService;
import com.example.baapp.photo.PhotoService;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class DialogHelper {

    public static void showLocationRegistrationDialog(MainActivity activity) {
        DialogHelper dialog = new DialogHelper(
                activity,
                activity.getLocationService(),
                activity::refreshMarkers
        );
        dialog.show();
    }

    private final Context context;
    private final LocationService locationService;
    private final PhotoService photoService;

    private Spinner categorySpinner;
    private EditText subCategoryField;
    private EditText latitudeField;
    private EditText longitudeField;
    private EditText timestampField;
    private EditText memoField;

    private ImageView photoPreview;
    private Uri photoUri;
    private File photoFile;
    public interface OnLocationSavedListener {
        void onLocationSaved();
    }
    private final OnLocationSavedListener listener;

    public DialogHelper(Context context,
                        LocationService locationService,
                        OnLocationSavedListener listener) {
        this.context = context;
        this.locationService = locationService;
        this.photoService = PhotoService.getInstance(context);
        this.listener = listener;
    }

    private void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_registration, null);
        builder.setView(dialogView);

        categorySpinner = dialogView.findViewById(R.id.categorySpinner);

        ArrayAdapter<MainCategory> adapter =
                new ArrayAdapter<MainCategory>(
                        context,
                        android.R.layout.simple_spinner_item,
                        MainCategory.values()
                ) {
                    @NonNull
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        tv.setText(context.getString(Objects.requireNonNull(getItem(position)).getLabel()));
                        return view;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        tv.setText(context.getString(Objects.requireNonNull(getItem(position)).getLabel()));
                        return view;
                    }
                };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        subCategoryField = dialogView.findViewById(R.id.subCategoryEditText);

        latitudeField = dialogView.findViewById(R.id.latitudeField);
        longitudeField = dialogView.findViewById(R.id.longitudeField);
        timestampField = dialogView.findViewById(R.id.timestampField);
        memoField = dialogView.findViewById(R.id.memoField);

        Button btnAddPhoto = dialogView.findViewById(R.id.btnAddPhoto);
        photoPreview = dialogView.findViewById(R.id.photoPreview);

        btnAddPhoto.setOnClickListener(v -> showPhotoOptionDialog());

        initializeDialogFields();

        builder.setPositiveButton(context.getString(R.string.btn_register), (dialog, which) -> saveLocation());
        builder.setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

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
                    latitudeField.setText(context.getString(R.string.error_fail_get));
                    longitudeField.setText(context.getString(R.string.error_fail_get));
                    timestampField.setText("");
                }
            });
        }
    }

    private void setLocationFields(GeoPoint location) {
        latitudeField.setText(String.format(Locale.getDefault(), "%.6f", location.getLatitude()));
        longitudeField.setText(String.format(Locale.getDefault(), "%.6f", location.getLongitude()));

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        timestampField.setText(timestamp);
    }

    private void saveLocation() {
        MainCategory selected =
                (MainCategory) categorySpinner.getSelectedItem();
        String category =
                selected != null ? selected.getId() : MainCategory.DAILY.getId();
        String subCategory = subCategoryField.getText().toString();
        String timestamp = timestampField.getText().toString();
        String memo = memoField.getText().toString();
        boolean uploadFlg = false;
        String uriStr = photoUri != null ? photoUri.toString() : "";

        try {
            double latitude = Double.parseDouble(latitudeField.getText().toString());
            double longitude = Double.parseDouble(longitudeField.getText().toString());

            locationService.saveLocation(category, subCategory, latitude, longitude, timestamp, memo, uploadFlg, uriStr);
            Toast.makeText(
                    context,
                    context.getString(R.string.complete_register) + category,
                    Toast.LENGTH_SHORT
            ).show();

            if (listener != null) {
                listener.onLocationSaved();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(context, R.string.error_invalid_location, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.error_fail_register, Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhotoOptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String[] options = {context.getString(R.string.take_photo), context.getString(R.string.select_picture), context.getString(R.string.cancel)};

        builder.setTitle(context.getString(R.string.add_picture))
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
                                            Toast.makeText(context, R.string.error_read_picture, Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(context, R.string.error_process_picture, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (IOException e) {
                                Toast.makeText(context, R.string.error_launch_camera, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.error_read_picture, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(context, R.string.error_process_picture, Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;

                        default:
                            dialog.dismiss();
                    }
                }).show();
    }

}
