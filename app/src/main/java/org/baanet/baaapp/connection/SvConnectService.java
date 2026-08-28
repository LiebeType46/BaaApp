package org.baanet.baaapp.connection;

import android.content.Context;

import org.baanet.baaapp.Csv.CsvExporter;
import org.baanet.baaapp.data.AppDatabase;
import org.baanet.baaapp.data.LocationEntity;
import org.baanet.baaapp.common.UserDataScope;

import java.io.File;
import java.util.List;

public class SvConnectService {
    public static void upload(Context context) {

        // uploadFlgが立っていないデータを一括所得
        String ownerPublicId = UserDataScope.getCurrentPublicId(context);
        List<LocationEntity> unuploadedLocations = ownerPublicId != null
                ? AppDatabase.getInstance(context).locationDao().getUnuploadedLocationsByOwner(ownerPublicId)
                : AppDatabase.getInstance(context).locationDao().getUnownedUnuploadedLocations();

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
