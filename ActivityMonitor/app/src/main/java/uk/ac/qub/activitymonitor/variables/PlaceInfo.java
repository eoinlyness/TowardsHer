package uk.ac.qub.activitymonitor.variables;

import android.location.Address;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class PlaceInfo {
    public ArrayList<String> address;
    public String time;
    public String status;

    public PlaceInfo(ArrayList<String> address, String timeVisited, String status) {
        this.address = address;
        this.time = timeVisited;
        this.status = status;
    }

    public PlaceInfo(Address address, String timeVisited, String status) {
        this.address = new ArrayList<String>();
        int maxAddressLineIndex = address.getMaxAddressLineIndex();
        for (int i = 0; i <= maxAddressLineIndex; i++)
            this.address.add(address.getAddressLine(i));

        this.time = timeVisited;
        this.status = status;
    }

    public static String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        return format.format(Calendar.getInstance().getTime());
    }

}
