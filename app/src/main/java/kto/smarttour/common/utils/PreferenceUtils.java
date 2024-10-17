package kto.smarttour.common.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shared Preference 유틸.
 *
 * @author 전창현
 * @date 2013-12-27
 */
public class PreferenceUtils {

    /**
     * String형 저장.
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public static void setPreference(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Boolean형 저장.
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public static void setPreference(Context context, String key, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Integer형 저장.
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public static void setPreference(Context context, String key, int value) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Gets the preference string.
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @return the preference string
     */
    public static String getPreferenceString(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    /**
     * Gets the preference boolean.
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @param defaultValue
     *            the default value
     * @return the preference boolean
     */
    public static boolean getPreferenceBoolean(Context context, String key,
											   boolean defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        return prefs.getBoolean(key, defaultValue);
    }

    /**
     * Gets the preference int.
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @param defaultValue
     *            the default value
     * @return the preference int
     */
    public static int getPreferenceInt(Context context, String key,
									   int defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        return prefs.getInt(key, defaultValue);
    }

    /**
     * Set<String> value 추가
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public synchronized static void addPreference(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> list = getPreferenceList(context, key);
        list.add(value);
        clear(context, key);
        editor.putStringSet(key, list).apply();
    }

    /**
     * Set<String> value 삭제
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @param value
     *            the value
     */
    public synchronized static void removePreference(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> list = getPreferenceList(context, key);
        list.remove(value);
        clear(context, key);
        editor.putStringSet(key, list).apply();
    }

    /**
     * Gets the preference Set<String>.
     *
     * @param context
     *            the context
     * @param key
     *            the key
     * @return the preference int
     */
    public synchronized static Set<String> getPreferenceList(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        return prefs.getStringSet(key, new LinkedHashSet<>());
    }

    public synchronized static void clear(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName(), Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }
}
