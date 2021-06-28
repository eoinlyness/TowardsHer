package uk.ac.qub.activitymonitor.variables;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class CallInfo {

    public String number = "";
    public String name = "";
    public String time = "";
    public boolean outgoingCall = false;
    public int callLength = 0;
    public boolean missed = false;

    /**
     * New CallInfo
     *
     * @param number       Number of call
     * @param outgoingCall Out going call?
     * @param length       Length of call in seconds
     * @param time         Time when call started
     */
    public CallInfo(String number, String name, boolean outgoingCall, int length, String time) {
        this.number = number;
        this.name = name;
        this.outgoingCall = outgoingCall;
        callLength = length;
        this.time = time;
        missed = false;
    }

    /**
     * New CallInfo
     *
     * @param number       Number of call
     * @param outgoingCall Out going call?
     * @param length       Length of call in seconds
     */
    public CallInfo(String number, String name, boolean outgoingCall, int length) {
        this.number = number;
        this.name = name;
        this.outgoingCall = outgoingCall;
        callLength = length;
        missed = false;
        time = getCurrentTime();
    }

    /**
     * New CallInfo
     *
     * @param number Number of Call
     * @param time   Time of Call
     */
    public CallInfo(String number, String name, String time) {
        this.number = number;
        this.name = name;
        outgoingCall = false;
        callLength = 0;
        this.time = time;
        missed = true;
    }


    /**
     * Return current time in specific format : HH:mm:ss
     *
     * @return String
     */
    public static String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        return format.format(Calendar.getInstance().getTime());
    }

}
