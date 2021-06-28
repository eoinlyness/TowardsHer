package uk.ac.qub.activitymonitor.functions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import android.util.Log;

import uk.ac.qub.activitymonitor.MonitorService;
import uk.ac.qub.activitymonitor.variables.LocationPoint;
import uk.ac.qub.activitymonitor.variables.PlaceInfo;

import java.io.IOException;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;


public class GetLocation {

    String provider = "";
    LocationManager locationManager;
    Geocoder geocoder;
    Context context;
    int positionTime = 30000;

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

            Log.d("ProviderDisabled", "" + provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            float locAccuracy = location.getAccuracy();
            String posStatus = "";
            //if accuracy > 10 - most likely indoors
            if (locAccuracy > 10)
                posStatus = "indoors";
            else
                posStatus = "outdoors";

            Address closestAddress = getClosestAddress(location);
            if (closestAddress != null) {
                String time = PlaceInfo.getCurrentTime();
                MonitorService.serviceVariables.places.add(new PlaceInfo(closestAddress, time, posStatus));

                Log.d("LocationListener", "Latitude: " + location.getLatitude() + "  Longitude: " + location.getLongitude());
                MonitorService.serviceVariables.path.add(new LocationPoint(location, time));
            }
        }
    };

    public GetLocation(Context context) {
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        provider = locationManager.getBestProvider(criteria, true);

        Log.d("provider", provider);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("GetLocation", "Requesting Start");
            locationManager.requestLocationUpdates(provider, positionTime, 30, locationListener);
        }

        this.context = context;
        this.geocoder = new Geocoder(context);
    }

    /**
     * Get closest street address from current location
     * @param location current location
     * @return Address object containing nearest address
     */
    public Address getClosestAddress(Location location) {
        List<Address> addresses;
        try {
            //Get top 3 closest addresses based on location coordinates
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 3);
        } catch (IOException e) {
            return null;
        }

        int smallestDistanceIndex = -1;
        float smallestDistance = -1;
        float[] results = new float[1];
        //Compare the coordinates of each address to the current location to find the closest address
        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            Location.distanceBetween(address.getLatitude(), address.getLongitude(), location.getLatitude(), location.getLongitude(), results);
            float distance = results[0];
            if (smallestDistance == -1 || distance < smallestDistance) {
                smallestDistanceIndex = i;
                smallestDistance = distance;
            }
        }

        //Return the closest address
        return addresses.get(smallestDistanceIndex);
    }

    public void stopGetLocation() {
        locationManager.removeUpdates(locationListener);
    }

}
