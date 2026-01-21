package kto.smarttour.common.utils;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kto.smarttour.service.PlayerService;
import kto.smarttour.service.TaxiPlayerService;

public class SystemUtils {

	public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	/*
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
				PackageManager.PERMISSION_GRANTED) {
			// FCM SDK (and your app) can post notifications.
		} else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
			// TODO: display an educational UI explaining to the user the features that will be enabled
			//       by them granting the POST_NOTIFICATION permission. This UI should provide the user
			//       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
			//       If the user selects "No thanks," allow the user to continue without notifications.
		} else {
			// Directly ask for the permission
			requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
		}
	}
	*/

	/*
	public static final String[] PERMISSION_REQUEST_LIST = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };
	public static String[] getPermissionRequestList() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			//Log.d("TAG","getPermissionRequestList over TIRAMISU" );
			//POST_NOTIFICATIONS
			return new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.POST_NOTIFICATIONS };
		}
		return PERMISSION_REQUEST_LIST;
	}
	*/
	public static String[] getPermissionRequestList() {

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			// Photo Picker API 사용으로 미디어 권한 불필요
			return new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };
		} else {
			return new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };
		}
	}

	//지오팬스 백그라운드 (PERMISSION_REQUEST_LIST_Q (10)이상 권한 체크용로만 사용, 권한 요청용으로 안드로이드 R(11) 이상에서는 포그라운드 위치 정보 액세스 권한과 백그라운드 위치 정보 액세스 권한을 동시에 요청하면 시스템이 요청을 무시하고 앱에 어떤 권한도 부여하지 않습니다.
	//public static final String[] PERMISSION_REQUEST_LIST_Q = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
	//public static final String[] PERMISSION_REQUEST_LIST_Q = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

	//안드로이드 11 Build.VERSION_CODES.R 부터는 ACCESS_FINE_LOCATION
	//주의: 앱이 Android 11(API 수준 30) 이상을 타겟팅하면 시스템에서는 이 권장사항을 적용합니다. 포그라운드 위치 정보 액세스 권한과 백그라운드 위치 정보 액세스 권한을 동시에 요청하면 시스템이 요청을 무시하고 앱에 어떤 권한도 부여하지 않습니다.
	//public static final String[] PERMISSIOldN_REQUEST_LIST_R = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

	public static final String[] PERMISSION_REQUEST_GPS = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };

	/**
	 * 권한 체크
	 *
	 * @param act
	 * @param checkPermission
	 * @return
	 */
	public static ArrayList<String> checkSelfPermission(AppCompatActivity act, String[] checkPermission) {

		ArrayList<String> noPermissionList = new ArrayList<>();

		for (String permission : checkPermission) {
			if (ContextCompat.checkSelfPermission(act, permission) == PackageManager.PERMISSION_GRANTED) {
				continue;
			} else {
				noPermissionList.add(permission);
			}
		}
		return noPermissionList;
	}

	public static String getCurrentTime() {
		long now = System.currentTimeMillis();
		Date date = new Date(now);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}

	public static String sec2minsec(String time) {
		try {
			int sec = Integer.valueOf(time);
			return String.format("%02d:%02d", sec / 60, sec % 60);
		} catch (Exception e) {
			return "00:00";
		}

	}

	public static void setPlayerServiceEnable(Context context) {
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName(context, PlayerService.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
		pm.setComponentEnabledSetting(new ComponentName(context, TaxiPlayerService.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
	}

	public static void setTaxiPlayerServiceEnabled(Context context) {
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName(context, TaxiPlayerService.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
		pm.setComponentEnabledSetting(new ComponentName(context, PlayerService.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

	}

	//    public static boolean getScreenReaderOn(Context context) {
	//        AccessibilityManager am = context.getSystemService(Context.ACCESSIBILITY_SERVICE);
	//    }


	public static boolean checkGps(Context context) {
		LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		boolean gps_enabled = false;

		try {
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {
		}

		return gps_enabled;
	}
}
