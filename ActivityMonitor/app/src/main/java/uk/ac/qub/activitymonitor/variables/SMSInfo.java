package uk.ac.qub.activitymonitor.variables;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SMSInfo {

    public String body = "";
    public String from = "";
    public String name = "";
    public String time = "";

    /**
     * @param from Number from you received SMS
     * @param body Body of SMS
     * @param time Time of received SMS
     */
    public SMSInfo(String from, String name, String body, String time) {
        this.body = body;
        this.from = from;
        this.name = name;
        this.time = time;
    }

    /**
     * @param from Number from you received SMS
     * @param body Body of SMS
     */
    public SMSInfo(String from, String name, String body) {
        this.body = body;
        this.from = from;
        this.name = name;
    }

    /**
     * Return current time in specific format : EEE MMM dd HH:mm:ss z yyyy
     *
     * @return String
     */
    public static String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        return format.format(Calendar.getInstance().getTime());
    }


}
