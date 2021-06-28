package uk.ac.qub.activitymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import uk.ac.qub.activitymonitor.variables.CallInfo;
import uk.ac.qub.activitymonitor.variables.SMSInfo;

public class ServiceReceiver extends BroadcastReceiver {

    private boolean ringing = true, start = false, outgoing = false;
    private long sTime = 0, eTime;
    private String number = "";


    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            Log.d("Service", "Broadcast received something: " + intent.getAction());
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                Log.d("BROADCAST", "Received");

                Bundle intentExtras = intent.getExtras();

                if (intentExtras != null) {

                    Object[] sms = (Object[]) intentExtras.get("pdus");

                    for (int i = 0; i < sms.length; ++i) {


                        SmsMessage smsMessage;


                        SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                        smsMessage = msgs[0];

                        String phone = smsMessage.getOriginatingAddress();
                        String name = getContactbyPhoneNumber(context, phone);
                        String message = smsMessage.getMessageBody().toString();

                        MonitorService.serviceVariables.smsList.add(new SMSInfo(phone, name, message, SMSInfo.getCurrentTime()));
                        MonitorService.specialVariables.smsReceivedAvg++;

                        Log.d("SMSBROADCAST", "Phone: " + phone + "     Message: " + message);
                    }
                }

            } else if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
                Log.d("BROADCAST", "Outgoing call to: " + intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
                start = true;
                outgoing = true;
                sTime = System.currentTimeMillis();
                number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

            } else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                //OFFHOOK IDLE RINGING
                if (state.equals("OFFHOOK")) {
                    if (!start) {
                        start = true;
                        outgoing = false;
                        sTime = System.currentTimeMillis();
                    }
                } else if (state.equals("RINGING")) {
                    ringing = true;
                    number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Log.d("Ringing", "" + intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
                } else if (state.equals("IDLE")) { //OFFHOOK IDLE

                    String name = getContactbyPhoneNumber(context, number);

                    if (start) {
                        eTime = System.currentTimeMillis();

                        MonitorService.serviceVariables.callList.add(new CallInfo(number, name, outgoing, (int) ((eTime - sTime) / 1000), CallInfo.getCurrentTime()));

                        start = false;
                        ringing = false;

                        if (outgoing)
                            MonitorService.specialVariables.callTimeWeekOut += (int) ((eTime - sTime) / 1000);
                        else
                            MonitorService.specialVariables.callTimeWeekIn += (int) ((eTime - sTime) / 1000);

                        Log.d("New Call added", "Number: " + number + "  Outgoing: " + outgoing + "  Time: " + (int) ((eTime - sTime) / 1000));
                    } else {
                        if (ringing) {
                            MonitorService.serviceVariables.callList.add(new CallInfo(number, name, CallInfo.getCurrentTime()));
                            Log.d("New Missed Call added", "MISSED Number: " + number);
                            ringing = false;
                        }
                    }
                }
                Log.d("BROADCAST", "call state changed....: " + state);
            }

        } catch (Exception ex) {
            Log.d("ErrorServiceReceiver", "" + ex);
        }

    }

    public String getContactbyPhoneNumber(Context context ,String phoneNumber) {

        android.net.Uri uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber));
        String[] projection = {android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME};
        android.database.Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor == null) {
            return phoneNumber;
        }else {
            String name = phoneNumber;
            try {

                if (cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME));
                }

            } finally {
                cursor.close();
            }

            return name;
        }
    }

}
