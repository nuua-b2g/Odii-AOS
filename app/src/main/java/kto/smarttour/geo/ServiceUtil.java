package kto.smarttour.geo;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ServiceUtil {

	public static synchronized boolean isRunning(Context context, Class<?> serviceClass) {
		if (serviceClass == null)
			return false;
		
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (service.service.getClassName().equals(serviceClass.getName())) {
				return true;
			}
		}
		return false;
	}

	public static synchronized void startService(Context context, Class<?> serviceClazz) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				context.startForegroundService(new Intent(context, serviceClazz));
			} else {
				Intent i = new Intent(context, serviceClazz);
				context.startService(i);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static synchronized void stopService(Context context, Class<?> serviceClazz) {
		Intent i = new Intent(context, serviceClazz);
		context.stopService(i);
	}
}
