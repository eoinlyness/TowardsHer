package uk.ac.qub.activitymonitor.variables;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class URLInfo {
    public String url;
    public String time;

    public URLInfo(String url, long timeVisited) {
        this.url = url;
        this.time = formatTime(timeVisited);
    }

    public String formatTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        return format.format(new Date(time));
    }

}
