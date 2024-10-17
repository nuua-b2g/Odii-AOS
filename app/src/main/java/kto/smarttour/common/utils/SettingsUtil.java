package kto.smarttour.common.utils;

import android.content.Context;

/**
 * 환경 설정 관련 유틸.
 *
 * @author Changhyun Jeon
 * @since 2015. 6. 18
 */
public class SettingsUtil {

	/**
	 * The Constant PREF_DATA_NETWORK.
	 */
	private final static String PREF_DATA_NETWORK = "dataNetwork";

	/**
	 * The Constant PREF_LOCALE.
	 */
	private final static String PREF_LOCALE = "locale";

	/**
	 * The Constant PREF_USE_GPS.
	 */
	private final static String PREF_USE_GPS = "useGps";

	private final static String PREF_USE_FCM = "useFcm";

	private final static String PREF_CHECK_PERMISSION = "checkPermission2";

	private final static String PREF_CHECK_PERMISSION_DIALOG_AGREE = "checkPermission2DialogAgree";

	private final static String PREF_TAXI_FIRST = "taxi.first";

	private final static String PREF_TAXI_TTID = "taxi.ttid";
	private final static String PREF_TAXI_LAST_INDEX = "taxi.last.index";

	private final static String PREF_DRIVE_MODE = "play.drive.mode";

	/**
	 * Checks if is use data network.
	 *
	 * @param context the context
	 * @return true, if is use data network
	 */
	public static boolean isUseDataNetwork(Context context) {
		return PreferenceUtils.getPreferenceBoolean(context, PREF_DATA_NETWORK, false);
	}

	/**
	 * Sets the use data network.
	 *
	 * @param context the context
	 * @param enabled the enabled
	 */
	public static void setUseDataNetwork(Context context, boolean enabled) {
		PreferenceUtils.setPreference(context, PREF_DATA_NETWORK, enabled);
	}

	/**
	 * Gets the locale.
	 *
	 * @param context the context
	 * @return the locale
	 */
	public static String getLocale(Context context) {
		return PreferenceUtils.getPreferenceString(context, PREF_LOCALE);
	}

	/**
	 * Sets the locale.
	 *
	 * @param context the context
	 * @param locale  the locale
	 */
	public static void setLocale(Context context, String locale) {
		PreferenceUtils.setPreference(context, PREF_LOCALE, locale);
	}

	/**
	 * Checks if is use gps.
	 *
	 * @param context the context
	 * @return true, if is use gps
	 */
	public static boolean isUseGps(Context context) {
		return PreferenceUtils.getPreferenceBoolean(context, PREF_USE_GPS, true);
	}

	/**
	 * Sets the use gps.
	 *
	 * @param context the context
	 * @param useGps  the use gps
	 */
	public static void setUseGps(Context context, boolean useGps) {
		PreferenceUtils.setPreference(context, PREF_USE_GPS, useGps);
	}


	public static void setUseFcm(Context context, boolean useFcm) {
		PreferenceUtils.setPreference(context, PREF_USE_FCM, useFcm);
	}

	public static boolean isUseFcm(Context context) {
		return PreferenceUtils.getPreferenceBoolean(context, PREF_USE_FCM, true);
	}

	public static void setCheckPermission(Context context, boolean checkPermission) {
		PreferenceUtils.setPreference(context, PREF_CHECK_PERMISSION, checkPermission);
	}

	public static boolean isCheckPermission(Context context) {
		return PreferenceUtils.getPreferenceBoolean(context, PREF_CHECK_PERMISSION, false);
	}

	//추가
	public static void setCheckPermissionDialogAgree(Context context, boolean checkPermission) {
		PreferenceUtils.setPreference(context, PREF_CHECK_PERMISSION_DIALOG_AGREE, checkPermission);
	}

	//추가
	public static boolean isCheckPermissionDialogAgree(Context context) {
		return PreferenceUtils.getPreferenceBoolean(context, PREF_CHECK_PERMISSION_DIALOG_AGREE, false);
	}

	public static boolean getTaxiFirstRun(Context context) {
		return PreferenceUtils.getPreferenceBoolean(context, PREF_TAXI_FIRST, true);
	}

	public static void setTaxiFirstRun(Context context, boolean isRun) {
		PreferenceUtils.setPreference(context, PREF_TAXI_FIRST, isRun);
	}

	public static int getTaxiTtid(Context context) {
		return PreferenceUtils.getPreferenceInt(context, PREF_TAXI_TTID, -1);
	}

	public static void setTaxiTtid(Context context, int ttid) {
		PreferenceUtils.setPreference(context, PREF_TAXI_TTID, ttid);
	}

	public static int getTexiLastIndex(Context context) {
		return PreferenceUtils.getPreferenceInt(context, PREF_TAXI_LAST_INDEX, 0);
	}

	public static void setTexiLastIndex(Context context, int index) {
		PreferenceUtils.setPreference(context, PREF_TAXI_LAST_INDEX, index);
	}

	public static void setDriveMode(Context context, boolean isDriveMode) {
		PreferenceUtils.setPreference(context, PREF_DRIVE_MODE, isDriveMode);
	}

	public static boolean getDriveMode(Context context) {
		return PreferenceUtils.getPreferenceBoolean(context, PREF_DRIVE_MODE, false);
	}

}
