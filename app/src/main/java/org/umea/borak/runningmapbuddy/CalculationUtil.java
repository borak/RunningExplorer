package org.umea.borak.runningmapbuddy;

import com.google.android.gms.maps.model.LatLng;

/**
 * This class handles utility calculations for the application. Currently, there's only support for
 * calculating distances between two instances of LatLng.class.
 */
public class CalculationUtil {

    /**
     * Calculating the distance difference between two instances of LatLng.class.
     * @param startPos The start position.
     * @param endPos The end position.
     * @return The distance between the given positions in kilometres.
     */
    public static double distance(LatLng startPos, LatLng endPos) {
        if(startPos == null || endPos == null) {
            return 0;
        }
        double lat1 = startPos.latitude;
        double lat2 = endPos.latitude;
        double lon1 = startPos.longitude;
        double lon2 = endPos.longitude;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return 6366000 * c;
    }

}
