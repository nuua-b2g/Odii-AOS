package kto.smarttour;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.naver.maps.map.NaverMapSdk;
import com.socks.library.KLog;

import java.util.ArrayList;

import kto.smarttour.common.utils.AnalyticsInterface;
import kto.smarttour.common.utils.ForegroundDetector;
import kto.smarttour.db.StampDBManager;
import kto.smarttour.geo.GeofenceController;
import kto.smarttour.geo.LocationConstants;
import kto.smarttour.geo.LocationService;
import kto.smarttour.geo.ServiceUtil;
import kto.smarttour.location.CurrentLocation;
import kto.smarttour.network.response.dao.StampEventList;
import kto.smarttour.service.PlayerService;
import kto.smarttour.ui.player.PlayListManager;

public class OdiiApplication extends MultiDexApplication implements ForegroundDetector.Listener {
	private static AppCompatActivity webActivity;
	private static Location location;

	private static Context context;

	public static String deviceCountryCode;
	public static String deviceLanguageCode;

	public static boolean isGeofenceNotifcationClick = false;
	public static String stamp_url = null;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(getApplicationContext());
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		context = getApplicationContext();

		FirebaseApp.initializeApp(context);

		KLog.init(BuildConfig.DEBUG);
		new CurrentLocation(this, loc -> OdiiApplication.setLocation(loc));
		PlayListManager.getInstance().init(getApplicationContext());

		//new ForegroundDetector(this);
		ForegroundDetector foregroundDetector = new ForegroundDetector(this);
		foregroundDetector.addListener(this);

		AnalyticsInterface.getInstance().init(getApplicationContext());
		Thread.setDefaultUncaughtExceptionHandler(new AppExceptionHandler());

		//	기존 - 지오팬스
		GeofenceController.getInstance().init(getApplicationContext());

		//	신규 - 위치 정보 포그라운드 서비스 시작

		//네이버지도 V3 클라이언트 ID 지정 (구)
		/*
		NaverMapSdk.getInstance(this).setClient(
				new NaverMapSdk.NaverCloudPlatformClient("jmn2agwecn"));
		*/

		//2023.07.17
		/**
		 * Application 이름 : VKMP-Maps
		Client ID (X-NCP-APIGW-API-KEY-ID) : 6bjpti0bj6
		Client Secret (X-NCP-APIGW-API-KEY) : y7RCi2arK8gyJNSjo44lATMkyWO7cAre743BmEAq
		 */
		NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformGovClient("6bjpti0bj6"));

	}

	public static AppCompatActivity getWebActivity() {
		return webActivity;
	}

	public static void setWebActivity(AppCompatActivity webActivity1) {
		webActivity = webActivity1;
	}

	public static Location getLocation() {
		return location;
	}

	public static void setLocation(Location location) {
		OdiiApplication.location = location;
	}

	class AppExceptionHandler implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			Log.i("ODII_ERROR", e.toString());
		}
	}

	@Override
	public void onBecameForeground() {
		//	서비스 시작
		if ((LocationConstants.isStampEventExisted || isStampEventExisted()) && !ServiceUtil.isRunning(context, LocationService.class)) {
			ServiceUtil.startService(context, LocationService.class);
		}

		if (PlayerService.isPlay) {

		}
	}

	@Override
	public void onBecameBackground() {
		//	서비스 종료
		boolean isServiceRunning = ServiceUtil.isRunning(getApplicationContext(), LocationService.class);
		if (isServiceRunning) {
			Intent intent = new Intent(getApplicationContext(), LocationService.class);
			intent.putExtra(LocationConstants.EXTRA_STRING_STOP_LOCATION_SERVICE, true);
			startService(intent);
		}

		if (PlayerService.isPlay) {

		}
	}

	public boolean isStampEventExisted() {
		ArrayList<StampEventList> stampEventList = StampDBManager.getInstance(context).getList();
		boolean isStampEventExisted = false;
		for (StampEventList stamp : stampEventList) {
			if ("N".equals(stamp.stamp_v_yn)) {
				isStampEventExisted = true;
				break;
			}
		}
		return isStampEventExisted;
	}

	public static Context getContext() {
		return context;
	}

	public static void finishApplication() {
		android.os.Process.killProcess(android.os.Process.myPid());
	}

}
