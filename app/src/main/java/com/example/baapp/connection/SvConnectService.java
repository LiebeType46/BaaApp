package com.example.baapp.connection;

import android.content.Context;

import com.example.baapp.Csv.CsvExporter;
import com.example.baapp.data.AppDatabase;
import com.example.baapp.data.LocationEntity;

import java.io.File;
import java.util.List;

public class SvConnectService {
    public static void upload(Context context) {

        // uploadFlgが立っていないデータを一括所得
        List<LocationEntity> unuploadedLocations = AppDatabase.getInstance(context).locationDao().getUnuploadedLocations();

        if (unuploadedLocations.isEmpty()) {
            return;
        }

        // CSV変換部品でファイル化（作成先は一時保存フォルダ）
        try {
            File csvFile = CsvExporter.export(unuploadedLocations, context.getCacheDir());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // サーバーに対してファイルを送信

        // 送信完了後にサーバー登録をリクエスト

        // サーバー登録完了後にフラグを立てる＆一時保存フォルダから削除
    }
}
