package kto.smarttour.common.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * check runtime permission
 * Created by ttb-02 on 2016-04-22.
 */
public class PermissionUtil {

    public static String[] REQ_LOCATION_PERMISSION = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };
    public static String[] REQ_WRITE_EXTERNAL_PERMISSION = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    /**
     * has permission
     *
     * @since 2016.04.22
     * @return {PERMISSION_GRANTED} if you have the
     *         permission, or {PERMISSION_DENIED} if not.
     */
    public static boolean hasPermission(Context context, String needPermission)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (ContextCompat.checkSelfPermission(context, needPermission)
                    != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
            return true;
        }
        else
        {
            return true;
        }
    }

    /**
     * verifypermissions
     * @param :each grantResult
     * @return each required permission has been granted
     */
    public static boolean verifyPermissions(int[] grantResults)
    {
        // At least one result must be checked.
        if (grantResults.length < 1)
        {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults)
        {
            if (result != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }
        return true;
    }
    /**
     * end add
     */

    public static void requestPermission(AppCompatActivity activity, String[] permissions, int resultCode)
    {
        ActivityCompat.requestPermissions(activity, permissions, resultCode);
    }
}
