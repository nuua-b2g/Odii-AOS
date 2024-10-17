package kto.smarttour.geo;

import kto.smarttour.BuildConfig;

public class LocationConstants {
    final public static float DISTANCE_SCAN_LOCATION = 5.0f;
    final public static long INTERVAL_SCAN_LOCATION = 5 * 60 * 1000L;

    final public static String EXTRA_STRING_STOP_LOCATION_SERVICE = BuildConfig.APPLICATION_ID + ".stop";

    final public static int NOTIFICATION_ID = 1001;
    final public static String NOTIFICATION_CHANNEL_ID = "Location_Usage_Alert";

    public static boolean isStampEventExisted = false;
}
