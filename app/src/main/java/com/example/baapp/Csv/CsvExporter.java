package com.example.baapp.Csv;

import com.example.baapp.data.LocationEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvExporter {

    public static File export(List<LocationEntity> entities, File outputDir) throws Exception {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String fileName = "locations_" + System.currentTimeMillis() + ".csv";
        File csvFile = new File(outputDir, fileName);

        try (OutputStreamWriter writer =
                     new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8)) {

            // ヘッダ
            writer.write("id,category,sub_category,latitude,longitude,timestamp,memo,photo_uri\n");

            for (LocationEntity e : entities) {
                writer.write(toCsvLine(e));
                writer.write("\n");
            }
        }

        return csvFile;
    }

    private static String toCsvLine(LocationEntity e) {
        return join(
                String.valueOf(e.getId()),
                e.getCategory(),
                e.getSubCategory(),
                String.valueOf(e.getLatitude()),
                String.valueOf(e.getLongitude()),
                e.getTimestamp(),
                e.getMemo(),
                e.getPhotoUri()
        );
    }

    /** CSV用 join（エスケープ込み） */
    private static String join(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(escape(values[i]));
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";

        boolean needQuote =
                value.contains(",") ||
                        value.contains("\"") ||
                        value.contains("\n") ||
                        value.contains("\r");

        String escaped = value.replace("\"", "\"\"");

        return needQuote ? "\"" + escaped + "\"" : escaped;
    }
}
