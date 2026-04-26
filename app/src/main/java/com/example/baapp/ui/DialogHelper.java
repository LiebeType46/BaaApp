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
import com.example.baapp.common.CategoryLabelResolver;
import com.example.baapp.common.LanguageService;
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
import java.util.HashSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class DialogHelper {

    private static final Integer[] SEARCH_RESULT_LIMIT_OPTIONS = {
            1,
            5,
            10,
            SearchCondition.DEFAULT_RESULT_LIMIT,
            100,
            200,
            500,
            1000
    };

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
        LanguageService language = LanguageService.get(activity);
        builder.setView(dialogView);
        builder.setTitle(language.t("dialog.search_condition.title"));

        TextView categoryValue = dialogView.findViewById(R.id.tvSearchCategoryValue);
        Spinner resultLimitSpinner = dialogView.findViewById(R.id.spSearchResultLimit);
        EditText subCategoryField = dialogView.findViewById(R.id.etSearchSubCategory);
        TextView fromText = dialogView.findViewById(R.id.tvSearchFrom);
        TextView toText = dialogView.findViewById(R.id.tvSearchTo);
        EditText keywordField = dialogView.findViewById(R.id.etSearchKeyword);
        EditText radiusField = dialogView.findViewById(R.id.etSearchRadius);
        CheckBox hasPhotoCheck = dialogView.findViewById(R.id.cbHasPhoto);
        CheckBox unsentOnlyCheck = dialogView.findViewById(R.id.cbUnsentOnly);

        SearchCondition currentCondition = activity.getCurrentSearchCondition();
        applySearchConditionLabels(language, dialogView);
        List<MainCategory> selectedCategories = getSelectedMainCategories(currentCondition.getCategories());
        setSearchCategoryValueText(activity, categoryValue, selectedCategories);
        setupResultLimitSpinner(activity, resultLimitSpinner, currentCondition.getResultLimit());
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
        categoryValue.setOnClickListener(v ->
                showSearchCategoryPicker(activity, categoryValue, selectedCategories)
        );

        dialogView.findViewById(R.id.tvClearSearchCategory)
                .setOnClickListener(v -> {
                    selectedCategories.clear();
                    setSearchCategoryValueText(activity, categoryValue, selectedCategories);
                });
        dialogView.findViewById(R.id.tvClearSearchSubCategory)
                .setOnClickListener(v -> subCategoryField.setText(""));
        dialogView.findViewById(R.id.tvClearSearchFrom)
                .setOnClickListener(v -> fromText.setText(""));
        dialogView.findViewById(R.id.tvClearSearchTo)
                .setOnClickListener(v -> toText.setText(""));
        dialogView.findViewById(R.id.tvClearSearchKeyword)
                .setOnClickListener(v -> keywordField.setText(""));
        dialogView.findViewById(R.id.tvClearSearchRadius)
                .setOnClickListener(v -> radiusField.setText(""));

        builder.setPositiveButton(language.t("common.apply"), null);
        builder.setNeutralButton(language.t("common.clear_all"), null);
        builder.setNegativeButton(language.t("common.cancel"), (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                SearchCondition condition = buildSearchCondition(
                        activity,
                        selectedCategories,
                        resultLimitSpinner,
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
                clearSearchConditionInputs(
                        selectedCategories,
                        categoryValue,
                        activity,
                        resultLimitSpinner,
                        subCategoryField,
                        fromText,
                        toText,
                        keywordField,
                        radiusField,
                        hasPhotoCheck,
                        unsentOnlyCheck
                );
            });
        });

        dialog.show();
    }

    private static List<MainCategory> getSelectedMainCategories(List<String> selectedCategoryIds) {
        Set<String> selectedIds = new HashSet<>(selectedCategoryIds);
        List<MainCategory> selectedCategories = new ArrayList<>();
        for (MainCategory category : MainCategory.values()) {
            if (selectedIds.contains(category.getId())) {
                selectedCategories.add(category);
            }
        }

        return selectedCategories;
    }

    private static void showSearchCategoryPicker(
            Context context,
            TextView categoryValue,
            List<MainCategory> selectedCategories
    ) {
        MainCategory[] categories = MainCategory.values();
        String[] labels = new String[categories.length];
        boolean[] checkedItems = new boolean[categories.length];
        List<MainCategory> pendingSelection = new ArrayList<>(selectedCategories);

        for (int i = 0; i < categories.length; i++) {
            labels[i] = CategoryLabelResolver.getLabel(context, categories[i]);
            checkedItems[i] = pendingSelection.contains(categories[i]);
        }

        new AlertDialog.Builder(context)
                .setTitle(LanguageService.get(context).t("dialog.search_condition.category"))
                .setMultiChoiceItems(labels, checkedItems, (dialog, which, isChecked) -> {
                    MainCategory category = categories[which];
                    if (isChecked && !pendingSelection.contains(category)) {
                        pendingSelection.add(category);
                    } else if (!isChecked) {
                        pendingSelection.remove(category);
                    }
                })
                .setPositiveButton(LanguageService.get(context).t("common.apply"), (dialog, which) -> {
                    selectedCategories.clear();
                    selectedCategories.addAll(pendingSelection);
                    setSearchCategoryValueText(context, categoryValue, selectedCategories);
                })
                .setNeutralButton(LanguageService.get(context).t("common.clear"), (dialog, which) -> {
                    selectedCategories.clear();
                    setSearchCategoryValueText(context, categoryValue, selectedCategories);
                })
                .setNegativeButton(LanguageService.get(context).t("common.cancel"), null)
                .show();
    }

    private static void setSearchCategoryValueText(
            Context context,
            TextView categoryValue,
            List<MainCategory> selectedCategories
    ) {
        if (selectedCategories.isEmpty()) {
            categoryValue.setText("");
            return;
        }

        List<String> labels = new ArrayList<>();
        for (MainCategory category : selectedCategories) {
            labels.add(CategoryLabelResolver.getLabel(context, category));
        }
        categoryValue.setText(String.join(", ", labels));
    }

    private static void setupResultLimitSpinner(Context context, Spinner spinner, Integer selectedLimit) {
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                SEARCH_RESULT_LIMIT_OPTIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int safeSelectedLimit = selectedLimit != null
                ? selectedLimit
                : SearchCondition.DEFAULT_RESULT_LIMIT;
        for (int i = 0; i < SEARCH_RESULT_LIMIT_OPTIONS.length; i++) {
            if (SEARCH_RESULT_LIMIT_OPTIONS[i] == safeSelectedLimit) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(getDefaultResultLimitPosition());
    }

    private static SearchCondition buildSearchCondition(
            Context context,
            List<MainCategory> selectedCategories,
            Spinner resultLimitSpinner,
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
                    Toast.makeText(
                            context,
                            LanguageService.get(context).t("dialog.search_condition.radius_positive"),
                            Toast.LENGTH_SHORT
                    ).show();
                    return null;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(
                        context,
                        LanguageService.get(context).t("dialog.search_condition.radius_number"),
                        Toast.LENGTH_SHORT
                ).show();
                return null;
            }
        }

        List<String> selectedCategoryIds = new ArrayList<>();
        for (MainCategory category : selectedCategories) {
            selectedCategoryIds.add(category.getId());
        }
        Integer resultLimit = (Integer) resultLimitSpinner.getSelectedItem();

        SearchCondition condition = new SearchCondition(
                null,
                subCategoryField.getText().toString(),
                fromText.getText().toString(),
                toText.getText().toString(),
                keywordField.getText().toString(),
                hasPhotoCheck.isChecked() ? Boolean.TRUE : null,
                unsentOnlyCheck.isChecked() ? Boolean.FALSE : null,
                radiusMeters,
                resultLimit
        );
        condition.setCategories(selectedCategoryIds);
        return condition;
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

    private static void applySearchConditionLabels(LanguageService language, View dialogView) {
        language.setText(dialogView, R.id.tvSearchCategoryLabel, "dialog.search_condition.category");
        language.setText(dialogView, R.id.tvClearSearchCategory, "common.clear");
        language.setHint(dialogView, R.id.tvSearchCategoryValue, "dialog.search_condition.unspecified");
        language.setText(dialogView, R.id.tvSearchResultLimitLabel, "dialog.search_condition.result_limit");
        language.setText(dialogView, R.id.tvSearchSubCategoryLabel, "dialog.search_condition.sub_category");
        language.setText(dialogView, R.id.tvClearSearchSubCategory, "common.clear");
        language.setHint(dialogView, R.id.etSearchSubCategory, "dialog.search_condition.sub_category_hint");
        language.setText(dialogView, R.id.tvSearchFromLabel, "dialog.search_condition.from");
        language.setText(dialogView, R.id.tvClearSearchFrom, "common.clear");
        language.setHint(dialogView, R.id.tvSearchFrom, "dialog.search_condition.unspecified");
        language.setText(dialogView, R.id.tvSearchToLabel, "dialog.search_condition.to");
        language.setText(dialogView, R.id.tvClearSearchTo, "common.clear");
        language.setHint(dialogView, R.id.tvSearchTo, "dialog.search_condition.unspecified");
        language.setText(dialogView, R.id.tvSearchKeywordLabel, "dialog.search_condition.memo_keyword");
        language.setText(dialogView, R.id.tvClearSearchKeyword, "common.clear");
        language.setHint(dialogView, R.id.etSearchKeyword, "dialog.search_condition.keyword_hint");
        language.setText(dialogView, R.id.tvSearchRadiusLabel, "dialog.search_condition.radius");
        language.setText(dialogView, R.id.tvClearSearchRadius, "common.clear");
        language.setHint(dialogView, R.id.etSearchRadius, "dialog.search_condition.unspecified");
        language.setText(dialogView, R.id.cbHasPhoto, "dialog.search_condition.has_photo");
        language.setText(dialogView, R.id.cbUnsentOnly, "dialog.search_condition.unsent_only");
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void clearSearchConditionInputs(
            List<MainCategory> selectedCategories,
            TextView categoryValue,
            Context context,
            Spinner resultLimitSpinner,
            EditText subCategoryField,
            TextView fromText,
            TextView toText,
            EditText keywordField,
            EditText radiusField,
            CheckBox hasPhotoCheck,
            CheckBox unsentOnlyCheck
    ) {
        selectedCategories.clear();
        setSearchCategoryValueText(context, categoryValue, selectedCategories);
        resultLimitSpinner.setSelection(getDefaultResultLimitPosition());
        subCategoryField.setText("");
        fromText.setText("");
        toText.setText("");
        keywordField.setText("");
        radiusField.setText("");
        hasPhotoCheck.setChecked(false);
        unsentOnlyCheck.setChecked(false);
    }

    private static int getDefaultResultLimitPosition() {
        for (int i = 0; i < SEARCH_RESULT_LIMIT_OPTIONS.length; i++) {
            if (SEARCH_RESULT_LIMIT_OPTIONS[i] == SearchCondition.DEFAULT_RESULT_LIMIT) {
                return i;
            }
        }
        return 0;
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
        LanguageService language = LanguageService.get(context);
        builder.setView(dialogView);
        builder.setTitle(language.t("dialog.register_location.title"));

        categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        applyLocationRegistrationLabels(language, dialogView);

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
                        tv.setText(CategoryLabelResolver.getLabel(context, Objects.requireNonNull(getItem(position))));
                        return view;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        tv.setText(CategoryLabelResolver.getLabel(context, Objects.requireNonNull(getItem(position))));
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

        builder.setPositiveButton(language.t("common.register"), (dialog, which) -> saveLocation());
        builder.setNegativeButton(language.t("common.cancel"), (dialog, which) -> dialog.dismiss());

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
                    latitudeField.setText(LanguageService.get(context).t("error.fail_get"));
                    longitudeField.setText(LanguageService.get(context).t("error.fail_get"));
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
                    LanguageService.get(context).t("register.complete") + category,
                    Toast.LENGTH_SHORT
            ).show();

            if (listener != null) {
                listener.onLocationSaved();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(context, LanguageService.get(context).t("error.invalid_location"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, LanguageService.get(context).t("error.fail_register"), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhotoOptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LanguageService language = LanguageService.get(context);
        String[] options = {
                language.t("photo.take_photo"),
                language.t("photo.select_picture"),
                language.t("common.cancel")
        };

        builder.setTitle(language.t("photo.add_picture"))
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
                                            Toast.makeText(context, language.t("error.read_picture"), Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(context, language.t("error.process_picture"), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (IOException e) {
                                Toast.makeText(context, language.t("error.launch_camera"), Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, language.t("error.read_picture"), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(context, language.t("error.process_picture"), Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;

                        default:
                            dialog.dismiss();
                    }
                }).show();
    }

    private void applyLocationRegistrationLabels(LanguageService language, View dialogView) {
        language.setText(dialogView, R.id.tvRegistrationCategoryLabel, "dialog.register_location.category");
        language.setText(dialogView, R.id.tvRegistrationSubCategoryLabel, "dialog.register_location.sub_category");
        language.setHint(dialogView, R.id.subCategoryEditText, "dialog.register_location.sub_category_hint");
        language.setHint(dialogView, R.id.latitudeField, "dialog.register_location.latitude_hint");
        language.setHint(dialogView, R.id.longitudeField, "dialog.register_location.longitude_hint");
        language.setHint(dialogView, R.id.timestampField, "dialog.register_location.timestamp_hint");
        language.setHint(dialogView, R.id.memoField, "dialog.register_location.memo_hint");
        language.setText(dialogView, R.id.btnAddPhoto, "dialog.register_location.add_photo");
    }

}
