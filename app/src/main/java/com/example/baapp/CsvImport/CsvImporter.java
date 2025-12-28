package com.example.baapp.CsvImport;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.baapp.data.AppDatabase;
import com.example.baapp.data.LocationEntity;

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
            List<LocationEntity> data = parseCsvLines(lines);
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

    private List<LocationEntity> parseCsvLines(List<String> lines) {
        List<LocationEntity> result = new ArrayList<>();
        boolean hasHeader = true;

        if (!lines.isEmpty()) {
            String first = lines.get(0).trim();
            if (isDataLine(first)) {
                hasHeader = false;
                result.add(toEntity(first));
            }
        }

        for (int i = hasHeader ? 1 : 0; i < lines.size(); i++) {
            LocationEntity entity = toEntity(lines.get(i));
            if (entity != null) result.add(entity);
        }
        return result;
    }

    private boolean isDataLine(String line) {
        String[] cols = smartCsvSplit(line);
        try {
            Double.parseDouble(cols[0]);
            Double.parseDouble(cols[1]);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private LocationEntity toEntity(String line) {
        String[] cols = smartCsvSplit(line);
        if (cols.length != 7) return null;
        try {
            // 各値に trim() を追加
            String category = cols[0].trim();
            String subCategory = cols[1].trim();
            double lat = Double.parseDouble(cols[2].trim());
            double lon = Double.parseDouble(cols[3].trim());
            String timestamp = cols[4].trim();
            String memo = cols[5].trim();
            String photoPath = cols[6].trim();
            return new LocationEntity(category, subCategory, lat, lon, timestamp, memo, photoPath);
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
}