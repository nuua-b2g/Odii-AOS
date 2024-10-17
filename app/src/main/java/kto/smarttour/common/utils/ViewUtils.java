package kto.smarttour.common.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicInteger;

import kto.smarttour.R;

public class ViewUtils {

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static int getDisplayHeight(AppCompatActivity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point.y;
    }

    public static int getDisplayWidth(AppCompatActivity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point.x;
    }

    /**
     * 현재 DEVICE의 기본 DISPLAY 가로 크기를 반환한다
     *
     * @param context
     *            어플리캐이션의 컨텍스트
     * @return Default Display Width
     */
    public static int getWidth(Context context) {
        DisplayMetrics outMetrics = new DisplayMetrics();
        getDefaultDisplay(context).getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * 현재 DEVICE의 기본 DISPLAY 세로 크기를 반환한다
     *
     * @param context
     *            어플리캐이션의 컨텍스트
     * @return Default Display Height
     */
    public static int getHeight(Context context) {
        DisplayMetrics outMetrics = new DisplayMetrics();
        getDefaultDisplay(context).getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    /**
     * 현재 DEVICE의 기본 DISPLAY를 반환한다.
     *
     * @param context
     *            어플리캐이션의 컨텍스트
     * @return Default Display
     */
    public static Display getDefaultDisplay(Context context) {
        return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    public static void setViewHeight(AppCompatActivity activity, View view, float percent) {
        int height = (int) (getDisplayHeight(activity) * percent);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height;
        view.setLayoutParams(params);
    }

    public static void setViewHeight(AppCompatActivity activity, View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height;
        view.setLayoutParams(params);
    }

    public static void setViewSizeChange(View view, int size) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = size;
        params.width = size;
        view.setLayoutParams(params);
    }

    public static int convertDip2Pixels(Context context, int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
    }

    public static int[] getResourceSize(Context context, int resourceId) {
        int[] size = {0, 0};

        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resourceId);
        size[0] = bm.getWidth();
        size[1] = bm.getHeight();
        bm.recycle();
        return size;
    }

    public static int[] getResourceSize(Context context, ImageView view) {
        int[] size = {0, 0};
        view.invalidate();
        view.buildDrawingCache();
        Bitmap bm = view.getDrawingCache();
        size[0] = bm.getWidth();
        size[1] = bm.getHeight();
        bm.recycle();
        return size;
    }

    private final static DisplayMetrics xDisplayMetrics = Resources.getSystem().getDisplayMetrics();
    private final static float density = xDisplayMetrics.density;

    public static int px2dp(int px) {
        return (int) (px / density);
    }

    public static int dp2px(int dp) {
        return (int) (dp * density);
    }

    public static int px2dp(float px) {
        return (int) (px / density);
    }

    public static int dp2px(float dp) {
        return (int) (dp * density);
    }

    public static float getDensity() {
        return density;
    }

    public static float getDPI() {
        return xDisplayMetrics.densityDpi;
    }

    public static int getScreenWidth() {
        return xDisplayMetrics.widthPixels;
    }

    public static int getScreenHeight() {
        return xDisplayMetrics.heightPixels;
    }

    @SuppressWarnings("deprecation")
    public static int getDisplayOrientation(Context context) {
        Display xDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        final int width = xDisplay.getWidth();
        final int height = xDisplay.getHeight();
        final int orientation = (height > width) ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
        return orientation;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getResourceHeight(Context context, int resource) {
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resource);
        int height = bm.getHeight();
        bm.recycle();
        return height;
    }

    public static int getResourceWidth(Context context, int resource) {
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resource);
        int width = bm.getWidth();
        bm.recycle();
        return width;
    }

    /*
     * 테블릿 구분
     * */
    public static boolean checkTabletDeviceWithScreenSize(Context context) {
        boolean device_large = ((context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >=
                Configuration.SCREENLAYOUT_SIZE_LARGE);

        if (device_large) {
            DisplayMetrics metrics = new DisplayMetrics();
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            return metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT
                    || metrics.densityDpi == DisplayMetrics.DENSITY_HIGH
                    || metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM
                    || metrics.densityDpi == DisplayMetrics.DENSITY_TV
                    || metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH;
        }
        return false;
    }

    public static void hideKeyboard(Context context, View view)
    {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null)
        {
            view.clearFocus();
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }

    public static void announceForAccessibility(Context context, String message) {
        AccessibilityManager manager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

        if (manager.isEnabled()) {

            AccessibilityEvent e = AccessibilityEvent.obtain();
            e.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            e.setClassName(context.getClass().getName());
            e.setPackageName(context.getPackageName());
            e.getText().add(message);

            manager.sendAccessibilityEvent(e);
        }
    }

    public static boolean isAccessibility(Context context) {
        AccessibilityManager manager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return manager.isEnabled();
    }
}
