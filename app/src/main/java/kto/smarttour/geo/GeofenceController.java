package kto.smarttour.geo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

import kto.smarttour.db.StampDBManager;
import kto.smarttour.network.response.dao.StampEventList;

public class GeofenceController {

	private int LOTERING_DELAY_TIME = 1; // 머무르는 시간
	private int RESPONSIVENESS_TIME = 1 * 60 * 1000; // 응답시간
	private Context context;
	//private GeofencingClient mGeofencingClient;
	//private PendingIntent mGeofencePendingIntent;

	//private ArrayList<Geofence> mGeofenceList = new ArrayList<>();
	private ArrayList<StampEventList> mGeofenceList = new ArrayList<>();

	@SuppressLint("StaticFieldLeak")
	private static GeofenceController INSTANCE;

	public static GeofenceController getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new GeofenceController();
		}
		return INSTANCE;
	}

	public void init(Context context) {
		this.context = context;

		//mGeofencingClient = LocationServices.getGeofencingClient(context);

		//	서비스 시작 - 테스트
//		if (!ServiceUtil.isRunning(context, LocationService.class)) {
//			ServiceUtil.startService(context, LocationService.class);
//		}
	}

	@SuppressLint("MissingPermission")
	public void setup(ArrayList<StampEventList> stampEventList) {
		mGeofenceList.clear();
		for (StampEventList stamp : stampEventList) {
			if ("N".equals(stamp.stamp_v_yn)) {
				mGeofenceList.add(stamp);
			}
		}

		int size = mGeofenceList.size();
		LocationConstants.isStampEventExisted = size > 0;
		if (size > 0) {
			//	서비스 시작
			if (!ServiceUtil.isRunning(context, LocationService.class)) {
				ServiceUtil.startService(context, LocationService.class);
			}
		}
		else {
			unregister(null);
		}

//		mGeofenceList.clear();
//		for (StampEventList stamp : stampEventList) {
//			if ("N".equals(stamp.stamp_v_yn)) {
//				mGeofenceList.add(stamp);
//				try {
//					Geofence geofence = new Geofence.Builder().setRequestId(String.format("%s%s", stamp.tlid, stamp.slid))    // id
//							.setCircularRegion(Double.valueOf(stamp.posY), Double.valueOf(stamp.posX), stamp.radius) // 좌표, 반경
//							.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // 진입시
//							.setLoiteringDelay(LOTERING_DELAY_TIME)
//							.setNotificationResponsiveness(RESPONSIVENESS_TIME)
//							.setExpirationDuration(Geofence.NEVER_EXPIRE).build();
//
//					mGeofenceList.add(geofence);
//				} catch (Exception e) {
//
//				}
//			}
//		}

//		if (!mGeofenceList.isEmpty()) {
//			mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent()).addOnSuccessListener(task -> {
//				KLog.i("GeofenceController", "addGeofences Success");
//			}).addOnFailureListener(task -> {
//				task.printStackTrace();
//				KLog.i("GeofenceController", "addGeofences Failure");
//			}).addOnCompleteListener(task -> {
//				if(task.isComplete()) {
//					KLog.i("GeofenceController", "addOnCompleteListener Complete");
//				} else {
//					KLog.i("GeofenceController", "addOnCompleteListener Failure");
//				}
//			});
//		}
	}

	@SuppressLint("MissingPermission")
	public void boot() {
		mGeofenceList.clear();
		ArrayList<StampEventList> stampEventList = StampDBManager.getInstance(context).getList();

		for (StampEventList stamp : stampEventList) {
			if ("N".equals(stamp.stamp_v_yn)) {
				mGeofenceList.add(stamp);
			}
		}
		int size = mGeofenceList.size();
		LocationConstants.isStampEventExisted = size > 0;
		if (size > 0) {
			//	서비스 시작
			if (!ServiceUtil.isRunning(context, LocationService.class)) {
				ServiceUtil.startService(context, LocationService.class);
			}
		}
		else {
			unregister(null);
		}

//		mGeofenceList.clear();
//		ArrayList<StampEventList> stampEventList = StampDBManager.getInstance(context).getList();
//
//		int count = 0;
//		for (StampEventList stamp : stampEventList) {
//			if ("N".equals(stamp.stamp_v_yn)) {
//				mGeofenceList.add(stamp);
//				try {
//				Geofence geofence = new Geofence.Builder().setRequestId(String.format("%s%s", stamp.tlid, stamp.slid))    // id
//						.setCircularRegion(Double.valueOf(stamp.posY), Double.valueOf(stamp.posX), stamp.radius) // 좌표, 반경
//						.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // 진입시
//						.setLoiteringDelay(LOTERING_DELAY_TIME)
//						.setNotificationResponsiveness(RESPONSIVENESS_TIME)
//						.setExpirationDuration(Geofence.NEVER_EXPIRE).build();
//
//				mGeofenceList.add(geofence);
//				}
//				catch (Exception e) {
//
//				}
//			}
//		}

//		if (!mGeofenceList.isEmpty()) {
//
//			mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent()).addOnSuccessListener(task -> {
//				KLog.i("GeofenceController", "addGeofences Success");
//			}).addOnFailureListener(task -> {
//				KLog.i("GeofenceController", "addGeofences Failure");
//			}).addOnCompleteListener(task -> {
//				if(task.isComplete()) {
//					KLog.i("GeofenceController", "addOnCompleteListener Complete");
//				} else {
//					KLog.i("GeofenceController", "addOnCompleteListener Failure");
//				}
//			});
//		}
	}

//	private GeofencingRequest getGeofencingRequest() {
//		GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
//		builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
//		builder.addGeofences(mGeofenceList);
//		return builder.build();
//	}

//	private PendingIntent getGeofencePendingIntent() {
//		if (mGeofencePendingIntent != null) {
//			return mGeofencePendingIntent;
//		}
//		Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
//		mGeofencePendingIntent = PendingIntentUtils.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//		return mGeofencePendingIntent;
//	}

	public void unregister(ArrayList<StampEventList> list) {
		//	전체 목록을 제거하는 용도로만 사용. 개별 처리 안함
		mGeofenceList.clear();
		ArrayList<StampEventList> stampEventList = StampDBManager.getInstance(context).getList();

		for (StampEventList stamp : stampEventList) {
			if ("N".equals(stamp.stamp_v_yn)) {
				mGeofenceList.add(stamp);
			}
		}
		int size = mGeofenceList.size();
		LocationConstants.isStampEventExisted = size > 0;
		if (size == 0) {
			if (ServiceUtil.isRunning(context, LocationService.class)) {
				Intent intent = new Intent(context, LocationService.class);
				intent.putExtra(LocationConstants.EXTRA_STRING_STOP_LOCATION_SERVICE, true);
				context.startService(intent);
			}
			LocationConstants.isStampEventExisted = false;
		}
//		if (!list.isEmpty()) {
//			List<String> geoId = new ArrayList();
//			for (StampEventList stamp : list) {
//				if ("N".equals(stamp.stamp_v_yn)) {
//					geoId.add(String.format("%s%s", stamp.tlid, stamp.slid));
//				}
//			}
//
//			try {
//				mGeofencingClient.removeGeofences(geoId).addOnSuccessListener(task -> {
//					KLog.i("GeofenceController", "removeGeofences Success");
//				}).addOnFailureListener(task -> {
//					KLog.i("GeofenceController", "removeGeofences Failure");
//				});
//			} catch (Exception e) {
//
//			}
//		}
	}
}
