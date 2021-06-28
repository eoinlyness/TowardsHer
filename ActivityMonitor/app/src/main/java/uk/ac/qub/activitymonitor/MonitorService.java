package uk.ac.qub.activitymonitor;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import uk.ac.qub.activitymonitor.functions.ClassSaver;
import uk.ac.qub.activitymonitor.functions.GetLocation;
import uk.ac.qub.activitymonitor.variables.ApplicationInformation;
import uk.ac.qub.activitymonitor.variables.SMSInfo;
import uk.ac.qub.activitymonitor.variables.ServiceVariables;
import uk.ac.qub.activitymonitor.variables.SpecialVariables;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class MonitorService extends Service {

    ClassSaver classSaver = new ClassSaver();
    ServiceReceiver serviceReceiver = new ServiceReceiver();
    Calendar calendar = Calendar.getInstance();
    Context tContext = this;
    short timerVar = 0;
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    File dirName;
    GetLocation getLocation;
    IntentFilter filter;
    String lastActivity = "";
    Timer timer = new Timer();

    ServiceObserver serviceObserver = new ServiceObserver(new Handler());
    ContentResolver contentResolver;

    public static ServiceVariables serviceVariables = new ServiceVariables();
    public static SpecialVariables specialVariables = new SpecialVariables();

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("TaskRemoved", "On Task Removed");
        Intent restartService = new Intent(getApplicationContext(),
                this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePI);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        contentResolver = this.getContentResolver();
        serviceObserver.contentResolver = contentResolver;
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, serviceObserver);

        filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_SENT");
        filter.addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction("android.intent.action.NEW_OUTGOING_CALL");

        registerReceiver(serviceReceiver, filter);

        createNotification();

        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotification();
        start();
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, 0);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            final String CHANNEL_ID = "noti_channel";
            CharSequence notiChannelName = "Service Notification";
            String notiChannelDescription = "Service running notification";

            NotificationChannel notiChannel;
            notiChannel = new NotificationChannel(CHANNEL_ID, notiChannelName, NotificationManager.IMPORTANCE_LOW);
            notiChannel.setDescription(notiChannelDescription);
            notiChannel.enableLights(true);
            notiChannel.setLightColor(Color.RED);
            notiChannel.enableVibration(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notiChannel);
            }

            Notification notification =
                    new Notification.Builder(this, CHANNEL_ID)
                            .setContentTitle("Service Running")
                            .setContentText("Recording activity...")
                            .setSmallIcon(R.drawable.tracker)
                            .setContentIntent(pendingIntent)
                            .build();

            int notificationID = 1;
            startForeground(notificationID, notification);
        }
    }


    private void start() {
        dirName = new File(this.getDir("data", Context.MODE_PRIVATE).getPath() + "/logs");

        Log.d("Service", "Service started");

        if (classSaver.fileExist("data", "special.dat", tContext)) {
            specialVariables = (SpecialVariables) classSaver.loadClassFromFile(specialVariables.getClass(), "data", "special.dat", tContext);
            Log.d("SpecialVariables", "Variables Loaded");
        } else {
            Log.d("SpecialVariables", "Not exists");
        }

        if (new File(dirName + "/day_" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + ".json").exists()) {
            serviceVariables = (ServiceVariables) classSaver.loadClassFromSpecificFile(serviceVariables.getClass(), new File(dirName + "/day_" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + ".json"));
            Log.d("Loading Var", "Loaded");
        } else
            Log.d("Loading Var", "Not Exist");


        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                checkService();
                saveAll();
                Log.d("Service", "Timer" + calendar.getTime());
                Log.d("Service", "FILE: " + "day_" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + ".json");

            }

        }, 200000, 200000);


        String currentHomePackage = "";

        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            currentHomePackage = resolveInfo.activityInfo.packageName;
            Log.d("homepackage", "HOMEPACKAGE: " + currentHomePackage);
        } catch (Exception ex) {
            Log.w("Error", "" + ex);
        }

        final String CURRENT_HOME_PACKAGE = currentHomePackage;

        Timer timer2 = new Timer();
        timer2.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {

                    String top = getTopApp();

                    if (top != null && !top.isEmpty() && !top.equals(CURRENT_HOME_PACKAGE) && !top.equals("com.android.settings") && !top.equals("com.android.packageinstaller")) {

                        //Monitor applications
                        if (!ApplicationInformation.isLastApp(serviceVariables.appInfo, top)) {

                            PackageManager packageManager = getApplicationContext().getPackageManager();
                            String appName = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(top, PackageManager.GET_META_DATA));
                            serviceVariables.appInfo.add(new ApplicationInformation(top, appName));
                        }
                    }
                } catch (Exception ex) {
                    Log.w("Error", "" + ex);
                }
            }

        }, 2000, 2000);

        Log.d("DATE", SMSInfo.getCurrentTime());

        try {
            getLocation = new GetLocation(this);
        } catch (Exception ex) {
            Log.w("Error", "" + ex);
        }
    }


    private String getTopApp() {
        String topPackageName = "";
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();

        // We get usage stats for the last 10 seconds
        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time);

        // Sort the stats by the last time used
        if (stats != null) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
            for (UsageStats usageStats : stats) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                topPackageName = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }

        return topPackageName;
    }

    private void checkService() {
        timerVar++;
        calendar = Calendar.getInstance();

        if (serviceVariables == null) {
            if (new File(dirName + "/day_" + calendar.get(Calendar.YEAR) + "_" + calendar.get(Calendar.DAY_OF_YEAR) + ".json").exists())
                serviceVariables = (ServiceVariables) classSaver.loadClassFromSpecificFile(serviceVariables.getClass(), new File(dirName + "/day_" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + ".dat"));
            else
                serviceVariables = new ServiceVariables();
        }

        if (specialVariables == null) {
            if (classSaver.fileExist("data", "special.dat", tContext))
                specialVariables = (SpecialVariables) classSaver.loadClassFromFile(specialVariables.getClass(), "data", "special.dat", tContext);
            else
                specialVariables = new SpecialVariables();
        }

        if (calendar.get(Calendar.DAY_OF_MONTH) != day) {
            day = calendar.get(Calendar.DAY_OF_MONTH);
            serviceVariables = new ServiceVariables();
        }

        if (timerVar > 2) {
            try {

                if (getLocation != null)
                    getLocation.stopGetLocation();

                getLocation = new GetLocation(this);
            } catch (Exception ex) {
                Log.w("Error", "" + ex);
            }

            specialVariables.resetAverage();
            timerVar = 0;
        }
    }


    public void saveAll() {
        calendar = Calendar.getInstance();

        if (!classSaver.saveClassToSpecificFile(serviceVariables, new File(dirName + "/day_" + calendar.get(Calendar.YEAR) + "_" + (calendar.get(Calendar.MONTH) + 1) + "_" + calendar.get(Calendar.DAY_OF_MONTH) + ".json"))) {
            Log.w("SaveAll", "Failed save variables!");
        }

        if (!classSaver.saveClassToSpecificFile(specialVariables, classSaver.getFile("data", "special.dat", tContext))) {
            Log.w("SaveAll", "Failed save special variables!");
        }
    }

    @Override
    public void onDestroy() {
        Log.d("Destroy", "Destroyed service");
        saveAll();
        unregisterReceiver(serviceReceiver);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
