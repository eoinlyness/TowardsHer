package uk.ac.qub.activitymonitor;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.provider.ContactsContract;
import android.content.Context;

import uk.ac.qub.activitymonitor.variables.SMSInfo;

import java.util.Calendar;

public class ServiceObserver extends ContentObserver {

    String lastSMS = "";
    public ContentResolver contentResolver;

    public ServiceObserver(Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        try {

            Uri uriSMSURI = Uri.parse("content://sms/sent");
            Cursor cur = contentResolver.query(uriSMSURI, null, null, null, null);
            cur.moveToNext();
            String content = cur.getString(cur.getColumnIndex("body"));
            String smsNumber = cur.getString(cur.getColumnIndex("address"));
            String contact = getContactbyPhoneNumber(smsNumber);


            Calendar calendar = Calendar.getInstance();
            try {
                calendar.setTimeInMillis(Long.parseLong(cur.getString(cur.getColumnIndex("date"))));
            } catch (Exception ex) {
                Log.w("Observer Error", "" + ex);
            }

            Log.d("Observer", "" + calendar.getTime());

            if (smsNumber == null || smsNumber.length() <= 0) {
                smsNumber = "Unknown";
            }
            cur.close();

            if (checkSMS("OutgoingSMS to " + smsNumber + ": " + content)) {
                Log.d("OBSERVERSMS", "OutgoingSMS to " + smsNumber + ": " + content);

                if (calendar.get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
                    MonitorService.serviceVariables.smsOutList.add(new SMSInfo(smsNumber, contact, content, SMSInfo.getCurrentTime()));
                    MonitorService.specialVariables.smsSentAvg++;

                } else {
                    Log.d("Observer", "SMS from yesterday");
                }
            }

        } catch (Exception ex) {
            Log.d("ErrorObserver", "" + ex);
        }

    }

    public String getContactbyPhoneNumber(String phoneNumber) {

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);

        if (cursor == null) {
            return phoneNumber;
        }else {
            String name = phoneNumber;
            try {

                if (cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                }

            } finally {
                cursor.close();
            }

            return name;
        }
    }


    public boolean checkSMS(String SMS) {

        if (SMS.equals(lastSMS) || SMS.equals(MonitorService.specialVariables.lastSentSMS)) {
            return false;
        } else {
            lastSMS = SMS;
        }
        return true;
    }


}
