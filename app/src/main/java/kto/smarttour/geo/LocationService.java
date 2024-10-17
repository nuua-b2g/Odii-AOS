package kto.smarttour.geo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

import kto.smarttour.OdiiApplication;
import kto.smarttour.R;
import kto.smarttour.common.utils.CommonUtils;
import kto.smarttour.common.utils.ForegroundDetector;
import kto.smarttour.common.utils.PendingIntentUtils;
import kto.smarttour.common.utils.SettingsUtil;
import kto.smarttour.common.utils.SystemUtils;
import kto.smarttour.db.StampDBManager;
import kto.smarttour.network.response.dao.StampEventList;
import kto.smarttour.ui.MainActivity;

public class LocationService extends Service {

    private static final String TAG = LocationService.class.getSimpleName();

    private LocationManager mLocationManager = null;

    private Handler mHandler = new Handler(Looper.myLooper());

    private boolean isStopService = false;

    private ArrayList<StampEventList> eventList = new ArrayList<>();

    private static LocationService instance;

    public static LocationService getInstance() {
        return LocationService.instance;
    }

    private IBinder binder = new LocationService.ServiceBinder();

    private static final long MIN_DELAY_TIME = 3000;
    private long mLastLocationTime;

    public class ServiceBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        LocationService.instance = LocationService.this;

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mHandler == null) {
            mHandler = new Handler(Looper.myLooper());
        }

        isStopService = intent.getBooleanExtra(LocationConstants.EXTRA_STRING_STOP_LOCATION_SERVICE, false);
        if (isStopService) {
            stopForeground(true);
            stopSelf();
        }

        eventList.clear();
        eventList.addAll(StampDBManager.getInstance(getApplicationContext()).getList());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                startForeground(LocationConstants.NOTIFICATION_ID, createNotification(getApplicationContext()));
            }
            catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        initializeLocationManager();


        return START_STICKY;
    }

    private void removeNotification() {
        try {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(LocationConstants.NOTIFICATION_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String message = context.getString(R.string.location_usage);
            String notificationChannelName = getString(R.string.disclosure_use_location_title);
            NotificationChannel serviceChannel = new NotificationChannel(LocationConstants.NOTIFICATION_CHANNEL_ID, notificationChannelName, NotificationManager.IMPORTANCE_HIGH);
            serviceChannel.setShowBadge(false);
            serviceChannel.setSound(null, null);
            serviceChannel.enableLights(false);
            serviceChannel.setLightColor(Color.BLUE);
            serviceChannel.setVibrationPattern(new long[]{0});
            serviceChannel.enableVibration(false);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            serviceChannel.setDescription(message);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification(Context context) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }
        else {
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        String message = context.getString(R.string.location_usage);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LocationConstants.NOTIFICATION_CHANNEL_ID);
        builder.setDefaults(Notification.DEFAULT_ALL);
        builder.setVibrate(new long[]{0});
        builder.setSmallIcon(R.drawable.icon);
        builder.setContentText(message);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setShowWhen(false);
        builder.setSilent(true);
        builder.setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(LocationConstants.NOTIFICATION_CHANNEL_ID);
        }
        return builder.build();
    }

    private void initializeLocationManager() {
        startLocationManager();
    }

    Runnable mRunnable = this::startLocationManager;

    private void startLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }

        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LocationConstants.INTERVAL_SCAN_LOCATION, LocationConstants.DISTANCE_SCAN_LOCATION, mLocationListeners[1]);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LocationConstants.INTERVAL_SCAN_LOCATION, LocationConstants.DISTANCE_SCAN_LOCATION, mLocationListeners[0]);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void stopLocationManager() {
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception e) {
                    Log.i(TAG, "fail to remove location listeners, ignore", e);
                }
            }
        }

        mLocationManager = null;

//        if (!isStopService) {
//            mHandler.postDelayed(mRunnable, LocationConstants.INTERVAL_SCAN_LOCATION);
//        }
    }

    private double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));

        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        if (unit.equals("kilometer")) {
            dist = dist * 1.609344;
        } else if (unit.equals("meter")) {
            dist = dist * 1609.344;
        }

        return (dist);
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        removeNotification();

        stopLocationManager();

        super.onDestroy();
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        private LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {

            //  3초 내 들어오는 위치 정보 무시 : 위치정보가 연달아서 같은 정보가 들어오는 경우
            long currentTime = SystemClock.uptimeMillis();
            long elapsedTime = currentTime - mLastLocationTime;
            mLastLocationTime = currentTime;
            if (elapsedTime <= MIN_DELAY_TIME) {
                return;
            }

            mLastLocation.set(location);

            double myLatitude = mLastLocation.getLatitude();
            double myLongitude = mLastLocation.getLongitude();

            for (StampEventList stamp : eventList) {
                String url = "";
                String tlid = "";
                String slid = "";
                double latitude = Double.parseDouble(stamp.posY);
                double longitude = Double.parseDouble(stamp.posX);
                double radius = stamp.radius;
                String stamp_id = String.format("%s%s", stamp.tlid, stamp.slid);
                double distance = distance(myLatitude, myLongitude, latitude, longitude, "meter");
                //  현재 좌표가 발도장 이벤트 반경 내에 존재하는 경우
                if (distance < radius) {
                    //  발도장 이벤트가 진행중인 경우
                    if (stamp.end_date > System.currentTimeMillis() && "N".equals(stamp.stamp_v_yn)) {
                        url = stamp.linkUrl;
                        tlid = String.valueOf(stamp.tlid);
                        slid = String.valueOf(stamp.slid);

                        //  노티를 띄움
                        Intent notiIntent = new Intent(getApplicationContext(), MainActivity.class);
                        notiIntent.putExtra("stamp", true);
                        notiIntent.putExtra("stamp_id", stamp_id);
                        notiIntent.putExtra("tlid", tlid);
                        notiIntent.putExtra("slid", slid);
                        notiIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                        if (ForegroundDetector.getInstance().isForeground()) {
                            // Foreground
                            notiIntent.setAction(EVENT_STAMP_ACTION);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notiIntent);
                        } else {
                            // Background
                            Context reContext;
                            try {
                                reContext = CommonUtils.changeLocaleLanguage2(getApplicationContext(), SettingsUtil.getLocale(getApplicationContext()));
                            } catch (Exception e) {
                                try {
                                    reContext = CommonUtils.changeLocaleLanguage2(OdiiApplication.getContext(), SettingsUtil.getLocale(getApplicationContext()));
                                } catch (Exception ee) {
                                    reContext = getApplicationContext();
                                }
                            }

                            if(TextUtils.isEmpty(url)) {
                                return;
                            }
                            notiIntent.putExtra("url", url);
                            sendNotification(reContext,reContext.getString(R.string.app_name), reContext.getString(R.string.stamp_push_content), notiIntent);
                        }
                    }
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }

    /**
     * 발도장 이벤트 도착 푸시 알림
     */
    public static final String EVENT_STAMP_ACTION = "odii.action.event_stamp";
    public static final int GEOFENCING_NOTIFICATION_ID = 482;
    private static final String CHANNEL_ID = "odii_01";

    private static void sendNotification(Context context, String title, String content, Intent intent) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int currentNighMode = OdiiApplication.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int colorCode = Color.BLACK;
        switch (currentNighMode) {
            case Configuration.UI_MODE_NIGHT_YES:
                colorCode = Color.WHITE;
                break;
            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            default:
                colorCode = Color.BLACK;
                break;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.app_name);
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        Intent notificationIntent = intent;
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent notificationPendingIntent = PendingIntentUtils.TaskStackBuilder_getPendingIntent(stackBuilder,0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.icon))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentText(content)
                .setColor(colorCode)
                .setContentIntent(notificationPendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }
        builder.setAutoCancel(true);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationManager.notify(GEOFENCING_NOTIFICATION_ID, builder.build());
    }
}
