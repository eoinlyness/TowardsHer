package uk.ac.qub.activitymonitor.variables;

import java.util.ArrayList;
import java.util.Calendar;


public class SpecialVariables {
    public int smsSentAvg = 0, smsReceivedAvg = 0, callTimeWeekIn = 0, callTimeWeekOut = 0;
    private boolean reset = false;
    public String lastSentSMS = "";

    public void resetAverage() {
        Calendar calendar = Calendar.getInstance();

        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY && !reset) {
            smsSentAvg = 0;
            smsReceivedAvg = 0;
            callTimeWeekIn = 0;
            callTimeWeekOut = 0;
            reset = true;
        }

        if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            reset = false;
        }

    }
}
