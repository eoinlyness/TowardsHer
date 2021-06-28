package uk.ac.qub.activitymonitor.variables;

import android.location.Location;

import org.osmdroid.util.GeoPoint;


public class LocationPoint {

    public float latitude = 0;
    public float longitude = 0;
    public String time = "";

    /**
     * New LocationPoint from latitude, longitude
     *
     * @param latitude  Latitude
     * @param longitude Longitude
     */
    public LocationPoint(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        time = getCurrentTime();
    }

    /**
     * New LocationPoint from Location
     *
     * @param location Location
     */
    public LocationPoint(Location location) {
        latitude = (float) location.getLatitude();
        longitude = (float) location.getLongitude();
        time = getCurrentTime();
    }

    public LocationPoint(Location location, String time) {
        latitude = (float) location.getLatitude();
        longitude = (float) location.getLongitude();
        this.time = time;
    }

    /**
     * LocationPoint to GeoPoint
     *
     * @return GeoPoint
     */
    public GeoPoint toGeoPoint() {
        return new GeoPoint(latitude, longitude);
    }

    public static String getCurrentTime() {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ENGLISH);
        return format.format(java.util.Calendar.getInstance().getTime());
    }

}
