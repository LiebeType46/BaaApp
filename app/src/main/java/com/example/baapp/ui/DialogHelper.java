package com.example.baapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.baapp.search.SearchCondition;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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

    public static void showSearchConditionDialog(MainActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = LayoutInflater.from(activity);
        View dialogView = inflater.inflate(R.layout.dialog_search_condition, null);
        builder.setView(dialogView);
        builder.setTitle("検索条件");

        Spinner categorySpinner = dialogView.findViewById(R.id.spSearchCategory);
        EditText subCategoryField = dialogView.findViewById(R.id.etSearchSubCategory);
        TextView fromText = dialogView.findViewById(R.id.tvSearchFrom);
        TextView toText = dialogView.findViewById(R.id.tvSearchTo);
        EditText keywordField = dialogView.findViewById(R.id.etSearchKeyword);
        EditText radiusField = dialogView.findViewById(R.id.etSearchRadius);
        CheckBox hasPhotoCheck = dialogView.findViewById(R.id.cbHasPhoto);
        CheckBox unsentOnlyCheck = dialogView.findViewById(R.id.cbUnsentOnly);

        SearchCondition currentCondition = activity.getCurrentSearchCondition();
        setupSearchCategorySpinner(activity, categorySpinner, currentCondition.getCategory());
        setTextIfPresent(subCategoryField, currentCondition.getSubCategory());
        setTextIfPresent(fromText, currentCondition.getFromTimestamp());
        setTextIfPresent(toText, currentCondition.getToTimestamp());
        setTextIfPresent(keywordField, currentCondition.getMemoKeyword());
        if (currentCondition.getRadiusMeters() != null) {
            radiusField.setText(String.valueOf(currentCondition.getRadiusMeters()));
        }
        hasPhotoCheck.setChecked(Boolean.TRUE.equals(currentCondition.getHasPhoto()));
        unsentOnlyCheck.setChecked(Boolean.FALSE.equals(currentCondition.getUploadFlg()));

        fromText.setOnClickListener(v -> showSearchDateTimePicker(activity, fromText));
        toText.setOnClickListener(v -> showSearchDateTimePicker(activity, toText));

        builder.setPositiveButton("適用", null);
        builder.setNeutralButton("クリア", null);
        builder.setNegativeButton(activity.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                SearchCondition condition = buildSearchCondition(
                        activity,
                        categorySpinner,
                        subCategoryField,
                        fromText,
                        toText,
                        keywordField,
                        radiusField,
                        hasPhotoCheck,
                        unsentOnlyCheck
                );

                if (condition == null) {
                    return;
                }

                activity.applySearchCondition(condition);
                dialog.dismiss();
            });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                activity.applySearchCondition(new SearchCondition());
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private static void setupSearchCategorySpinner(Context context, Spinner spinner, String selectedCategory) {
        List<String> categories = new ArrayList<>();
        categories.add("");
        for (MainCategory category : MainCategory.values()) {
            categories.add(category.name());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (selectedCategory != null) {
            int position = categories.indexOf(selectedCategory);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    private static SearchCondition buildSearchCondition(
            Context context,
            Spinner categorySpinner,
            EditText subCategoryField,
            TextView fromText,
            TextView toText,
            EditText keywordField,
            EditText radiusField,
            CheckBox hasPhotoCheck,
            CheckBox unsentOnlyCheck
    ) {
        Double radiusMeters = null;
        String radiusText = normalizeText(radiusField.getText().toString());
        if (radiusText != null) {
            try {
                radiusMeters = Double.parseDouble(radiusText);
                if (radiusMeters < 0) {
                    Toast.makeText(context, "半径は0以上で入力してください", Toast.LENGTH_SHORT).show();
                    return null;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(context, "半径は数値で入力してください", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        String selectedCategory = normalizeText(String.valueOf(categorySpinner.getSelectedItem()));

        return new SearchCondition(
                selectedCategory,
                subCategoryField.getText().toString(),
                fromText.getText().toString(),
                toText.getText().toString(),
                keywordField.getText().toString(),
                hasPhotoCheck.isChecked() ? Boolean.TRUE : null,
                unsentOnlyCheck.isChecked() ? Boolean.FALSE : null,
                radiusMeters
        );
    }

    private static void showSearchDateTimePicker(Context context, TextView target) {
        Calendar calendar = Calendar.getInstance();
        String currentText = normalizeText(target.getText().toString());
        if (currentText != null) {
            try {
                Date date = getSearchTimestampFormat().parse(currentText);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (ParseException ignored) {
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            context,
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);
                                target.setText(getSearchTimestampFormat().format(calendar.getTime()));
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private static SimpleDateFormat getSearchTimestampFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        format.setLenient(false);
        return format;
    }

    private static void setTextIfPresent(TextView view, String value) {
        if (value != null) {
            view.setText(value);
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
