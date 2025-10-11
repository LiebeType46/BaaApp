package com.example.baapp.map;

import android.location.Location;

import org.osmdroid.util.GeoPoint;

public class MapUtils {

    /**
     * 現在地と中心座標の距離を計算して、指定距離以内かどうか判定します。
     *
     * @param center   地図の中心座標
     * @param current  現在地
     * @param thresholdMeters 閾値（メートル）
     * @return true: 閾値以内 / false: 離れている
     */
    public static boolean isNearCenter(GeoPoint center, GeoPoint current, double thresholdMeters) {
        if (center == null || current == null) return false;

        float[] result = new float[1];
        Location.distanceBetween(
                center.getLatitude(), center.getLongitude(),
                current.getLatitude(), current.getLongitude(),
                result
        );
        return result[0] <= thresholdMeters;
    }
}
