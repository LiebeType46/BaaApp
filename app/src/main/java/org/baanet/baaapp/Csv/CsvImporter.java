package org.baanet.baaapp.Csv;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.baanet.baaapp.data.AppDatabase;
import org.baanet.baaapp.data.LocationEntity;
import org.baanet.baaapp.common.CategoryLabelResolver;
import org.baanet.baaapp.common.MainCategory;
import org.baanet.baaapp.common.MainCategoryConverter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CsvImporter {
    private final AppDatabase db;
    // CSV行を引用符付きで正しく分割する正規表現（カンマを引用符内では分割しない）
    private static final Pattern csvSplitPattern = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");


    public CsvImporter(AppDatabase db) {
        this.db = db;
    }

    public void importFromCsv(Context context, Uri fileUri) {
        try {
            List<String> lines = readCsvLines(context, fileUri);
            List<LocationEntity> data = parseCsvLines(context, lines);
            saveToDatabase(data);
        } catch (Exception e) {
            Log.e("CsvImporter", "インポート失敗: " + e.getMessage(), e);
        }
    }

    private List<String> readCsvLines(Context context, Uri uri) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getContentResolver().openInputStream(uri)))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
            return lines;
        }
    }

    private List<LocationEntity> parseCsvLines(Context context, List<String> lines) {
        List<LocationEntity> result = new ArrayList<>();
        boolean hasHeader = true;

        if (!lines.isEmpty()) {
            String first = lines.get(0).trim();
            if (isDataLine(first)) {
                hasHeader = false;
                result.add(toEntity(context, first));
            }
        }

        for (int i = hasHeader ? 1 : 0; i < lines.size(); i++) {
            LocationEntity entity = toEntity(context, lines.get(i));
            if (entity != null) result.add(entity);
        }
        return result;
    }

    private boolean isDataLine(String line) {
        String[] cols = smartCsvSplit(line);
        try {
            int offset = hasIdColumn(cols) ? 1 : 0;
            Double.parseDouble(cols[offset + 2].trim());
            Double.parseDouble(cols[offset + 3].trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private LocationEntity toEntity(Context context, String line) {
        String[] cols = smartCsvSplit(line);
        if (cols.length < 7) return null;
        try {
            int offset = hasIdColumn(cols) ? 1 : 0;
            String category = normalizeCategory(context, cols[offset].trim());
            String subCategory = cols[offset + 1].trim();
            double lat = Double.parseDouble(cols[offset + 2].trim());
            double lon = Double.parseDouble(cols[offset + 3].trim());
            String timestamp = cols[offset + 4].trim();
            String memo = cols[offset + 5].trim();
            boolean uploadFlg = true;
            String photoPath = cols.length > offset + 6 ? cols[offset + 6].trim() : "";
            return new LocationEntity(category, subCategory, lat, lon, timestamp, memo, uploadFlg, photoPath);
        } catch (Exception e) {
            Log.w("CsvImporter", "変換失敗: " + line);
            return null;
        }
    }


    private void saveToDatabase(List<LocationEntity> data) {
        db.locationDao().insertAll(data);
        Log.i("CsvImporter", "保存完了: " + data.size() + " 件");
    }

    private String[] smartCsvSplit(String line) {
        return csvSplitPattern.split(line);
    }

    private boolean hasIdColumn(String[] cols) {
        if (cols.length < 8) {
            return false;
        }

        try {
            Integer.parseInt(cols[0].trim());
            Double.parseDouble(cols[3].trim());
            Double.parseDouble(cols[4].trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeCategory(Context context, String value) {
        MainCategory category = CategoryLabelResolver.fromLabel(context, value);
        if (category != null) {
            return category.getId();
        }

        String categoryId = MainCategoryConverter.normalizeCategoryId(value);
        return categoryId != null ? categoryId : MainCategory.DAILY.getId();
    }
}
