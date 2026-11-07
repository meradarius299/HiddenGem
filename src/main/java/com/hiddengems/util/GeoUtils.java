package com.hiddengems.util;

/** Geographic helpers. */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private GeoUtils() {
    }

    /** Distance in meters between two points on Earth (Haversine formula). */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}