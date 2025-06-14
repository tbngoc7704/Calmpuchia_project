package com.calmpuchia.userapp;

import com.calmpuchia.userapp.model.User;
import com.calmpuchia.userapp.model.Store;

public class LocationUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Tính khoảng cách giữa 2 điểm theo công thức Haversine
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Kiểm tra store có gần user không (dựa vào lat/lng cache trong Realtime DB)
     */
    public static boolean isStoreNearUser(User user, Store store, double maxDistanceKm) {
        if (!user.isHasLocation()) {
            return false;
        }

        double distance = calculateDistance(
                user.getLat(), user.getLng(),
                store.getLat(), store.getLng()
        );

        return distance <= maxDistanceKm;
    }
}
