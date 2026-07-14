package com.example.syntheticfit.routing.domain;

public final class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoUtils() {}

    /** Haversine distance between two coordinates in meters. */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /** Decode a Google-encoded polyline string into lat/lon pairs. */
    public static double[][] decodePolyline(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new double[0][2];
        java.util.List<double[]> result = new java.util.ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result1 = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result1 |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result1 & 1) != 0 ? ~(result1 >> 1) : (result1 >> 1));
            lat += dlat;
            shift = 0;
            result1 = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result1 |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result1 & 1) != 0 ? ~(result1 >> 1) : (result1 >> 1));
            lng += dlng;
            result.add(new double[]{lat * 1e-5, lng * 1e-5});
        }
        return result.toArray(new double[0][2]);
    }

    /** Interpolate a coordinate on segment [p1, p2] at fraction t in [0,1]. */
    public static double[] interpolate(double lat1, double lon1, double lat2, double lon2, double t) {
        return new double[]{lat1 + (lat2 - lat1) * t, lon1 + (lon2 - lon1) * t};
    }
}
